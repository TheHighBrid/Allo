package com.kenza.callsim.voice

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import android.os.Process
import android.util.Log
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.concurrent.thread

/** Shared PCM format used by both supported live providers. */
object AudioConfig {
    const val SAMPLE_RATE = 16_000
    const val CHANNEL_IN = AudioFormat.CHANNEL_IN_MONO
    const val CHANNEL_OUT = AudioFormat.CHANNEL_OUT_MONO
    const val ENCODING = AudioFormat.ENCODING_PCM_16BIT
    const val BYTES_PER_SAMPLE = 2
    const val INPUT_CHUNK_BYTES =
        SAMPLE_RATE * GeminiLiveTuning.INPUT_CHUNK_MS / 1_000 * BYTES_PER_SAMPLE
}

/**
 * Captures 16 kHz mono PCM and forwards small chunks immediately. Speaker and
 * earpiece routes remain echo-safe half duplex, while headset microphone routes
 * stay full duplex so the user can genuinely interrupt Gemini mid-sentence.
 */
class MicRecorder(
    private val onChunk: (ByteArray) -> Unit,
    private val onError: (String) -> Unit = {},
    private val onStreaming: () -> Unit = {},
) {

    @Volatile var muted: Boolean = false
    @Volatile var agentSpeaking: Boolean = false
    @Volatile var speakerphoneOn: Boolean = false

    private var record: AudioRecord? = null
    @Volatile private var running = false
    private var worker: Thread? = null
    private var aec: AcousticEchoCanceler? = null
    private var ns: NoiseSuppressor? = null
    private var agc: AutomaticGainControl? = null
    private var lastRouteType: Int? = null

    @SuppressLint("MissingPermission")
    fun start() {
        if (running) return
        val minBuffer = AudioRecord.getMinBufferSize(
            AudioConfig.SAMPLE_RATE,
            AudioConfig.CHANNEL_IN,
            AudioConfig.ENCODING,
        )
        if (minBuffer <= 0) {
            onError("This device can't record 16 kHz mono audio.")
            return
        }

        // The AudioRecord allocation may be larger, but each network write stays
        // exactly 40 ms so Gemini receives a smooth realtime stream.
        val chunkBytes = AudioConfig.INPUT_CHUNK_BYTES
        val bufferSize = maxOf(minBuffer, chunkBytes * 2)

        val sources = intArrayOf(
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            MediaRecorder.AudioSource.MIC,
            MediaRecorder.AudioSource.DEFAULT,
        )
        for (source in sources) {
            val candidate = runCatching {
                AudioRecord(
                    source,
                    AudioConfig.SAMPLE_RATE,
                    AudioConfig.CHANNEL_IN,
                    AudioConfig.ENCODING,
                    bufferSize,
                )
            }.getOrNull()
            if (candidate != null && candidate.state == AudioRecord.STATE_INITIALIZED) {
                record = candidate
                Log.i(TAG, "AudioRecord initialized with source=$source chunkMs=${GeminiLiveTuning.INPUT_CHUNK_MS}")
                break
            }
            runCatching { candidate?.release() }
        }
        if (record == null) {
            onError("Microphone is unavailable (couldn't open an audio input).")
            return
        }

        record?.audioSessionId?.let { session ->
            if (AcousticEchoCanceler.isAvailable()) {
                aec = runCatching {
                    AcousticEchoCanceler.create(session)?.apply { enabled = true }
                }.getOrNull()
            }
            if (NoiseSuppressor.isAvailable()) {
                ns = runCatching {
                    NoiseSuppressor.create(session)?.apply { enabled = true }
                }.getOrNull()
            }
            if (AutomaticGainControl.isAvailable()) {
                agc = runCatching {
                    AutomaticGainControl.create(session)?.apply { enabled = true }
                }.getOrNull()
            }
            Log.i(TAG, "effects aec=${aec != null} ns=${ns != null} agc=${agc != null}")
        }

        running = true
        runCatching { record?.startRecording() }
            .onFailure {
                onError("Couldn't start the microphone: ${it.message}")
                running = false
                releaseAudioResources()
                return
            }

        var announced = false
        worker = thread(name = "mic-recorder") {
            val buffer = ByteArray(chunkBytes)
            while (running) {
                val read = record?.read(buffer, 0, buffer.size) ?: -1
                if (read > 0) {
                    if (!announced) {
                        announced = true
                        onStreaming()
                    }
                    val fullDuplex = routedInputSupportsBargeIn()
                    val echoSafeToSend = !agentSpeaking && !AudioPlaybackGate.isBlocked()
                    if (!muted && (fullDuplex || echoSafeToSend)) {
                        onChunk(buffer.copyOf(read))
                    }
                } else if (read < 0) {
                    running = false
                    onError("Microphone read error ($read).")
                    break
                }
            }
        }
    }

    /**
     * A dedicated headset mic is physically separated from the phone speaker, so
     * streaming it while Gemini speaks is safe and enables true barge-in. Built-in
     * mic routes remain gated to prevent the agent interrupting itself.
     */
    private fun routedInputSupportsBargeIn(): Boolean {
        val type = record?.routedDevice?.type ?: return false
        if (lastRouteType != type) {
            lastRouteType = type
            Log.i(TAG, "input route type=$type fullDuplex=${AudioRoutePolicy.allowsFullDuplex(type, speakerphoneOn)}")
        }
        return AudioRoutePolicy.allowsFullDuplex(type, speakerphoneOn)
    }

    fun stop() {
        running = false
        // AudioRecord.read() may block. Stopping the recorder first unblocks the
        // worker before resources are released, avoiding a thread/resource race.
        runCatching { record?.stop() }
        worker?.join(500)
        worker = null
        releaseAudioResources()
    }

    private fun releaseAudioResources() {
        runCatching { aec?.release() }
        aec = null
        runCatching { ns?.release() }
        ns = null
        runCatching { agc?.release() }
        agc = null
        runCatching { record?.release() }
        record = null
    }
}

/** Streams every valid agent PCM packet to the phone audio route in order. */
interface PlaybackMetrics {
    fun onQueued(queueMs: Int) {}
    fun onOverflow() {}
    /** The first sample of a new agent turn started playing (playback head passed it). */
    fun onPlaybackHeadAdvanced() {}
    /** AudioTrack starved while an agent turn was still streaming (audible stutter). */
    fun onUnderruns(delta: Int) {}
    /** An interruption flush has actually silenced the track. */
    fun onPlaybackStopped() {}
    companion object { val NOOP = object : PlaybackMetrics {} }
}

/**
 * Keeps built-in microphone routes gated until queued and hardware-buffered
 * playback has actually had time to finish. CallViewModel's visual speaking
 * timer is intentionally not trusted for echo safety.
 */
internal object AudioPlaybackGate {
    private val blockedUntilNanos = AtomicLong(0L)

    fun holdFor(durationMs: Int) {
        if (durationMs <= 0) return
        val candidate = System.nanoTime() + durationMs.toLong() * 1_000_000L
        blockedUntilNanos.updateAndGet { current -> maxOf(current, candidate) }
    }

    fun isBlocked(): Boolean = System.nanoTime() < blockedUntilNanos.get()

    fun clear() {
        blockedUntilNanos.set(0L)
    }
}

class PcmPlayer(
    private val sampleRate: Int = AudioConfig.SAMPLE_RATE,
    private val metrics: PlaybackMetrics = PlaybackMetrics.NOOP,
) {

    private class QueuedPcm(val epoch: Long, val pcm: ByteArray, val turnStart: Boolean = false)

    private var track: AudioTrack? = null
    private val queue = LinkedBlockingQueue<QueuedPcm>()
    private val queuedBytes = AtomicInteger()
    private val queueEpoch = AtomicLong()
    private val trackLock = Any()
    private val policy = PlaybackQueuePolicy(sampleRate)
    @Volatile private var running = false
    private val nextEnqueueStartsTurn = AtomicBoolean(true)
    private var worker: Thread? = null

    fun start() {
        if (track != null) return
        AudioPlaybackGate.clear()
        val minimumBuffer = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioConfig.CHANNEL_OUT,
            AudioConfig.ENCODING,
        )
        val targetBuffer =
            sampleRate * GeminiLiveTuning.OUTPUT_BUFFER_MS / 1_000 * AudioConfig.BYTES_PER_SAMPLE
        track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioConfig.CHANNEL_OUT)
                    .setEncoding(AudioConfig.ENCODING)
                    .build()
            )
            .setBufferSizeInBytes(maxOf(minimumBuffer, targetBuffer))
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        val requestedFrames = sampleRate * GeminiLiveTuning.OUTPUT_BUFFER_MS / 1_000
        val resized = runCatching { track?.setBufferSizeInFrames(requestedFrames) }.getOrNull()
        track?.play()
        Log.i(
            TAG,
            "AudioTrack buffer frames requested=$requestedFrames actual=${track?.bufferSizeInFrames} " +
                "capacity=${track?.bufferCapacityInFrames} resized=$resized underruns=${track?.underrunCount}",
        )
        running = true
        worker = thread(name = "pcm-playback", priority = Thread.MAX_PRIORITY) {
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
            val tracker = PlaybackTurnTracker(metrics)
            tracker.reset(headFrames(), track?.underrunCount ?: 0)
            while (running) {
                // While a turn's first sample is pending, poll so the head advance is noticed even
                // when no further packets arrive (short replies).
                val item = if (tracker.awaitingHeadAdvance) {
                    runCatching { queue.poll(HEAD_POLL_MS, TimeUnit.MILLISECONDS) }.getOrNull()
                } else {
                    runCatching { queue.take() }.getOrNull()
                }
                if (item == null) {
                    tracker.onHeadPosition(headFrames())
                    continue
                }
                val pcm = item.pcm
                if (pcm.isEmpty()) {
                    synchronized(trackLock) {
                        runCatching {
                            track?.pause()
                            track?.flush()
                            track?.play()
                        }
                    }
                    tracker.reset(headFrames(), track?.underrunCount ?: 0)
                    metrics.onPlaybackStopped()
                    continue
                }
                var written = 0
                synchronized(trackLock) {
                    if (running && item.epoch == queueEpoch.get()) {
                        tracker.beforeWrite(item.turnStart, track?.underrunCount ?: 0)
                        written = writeFully(pcm, item.epoch)
                    }
                }
                tracker.afterWrite(written / AudioConfig.BYTES_PER_SAMPLE, headFrames())
                if (item.epoch == queueEpoch.get()) {
                    queuedBytes.updateAndGet { (it - pcm.size).coerceAtLeast(0) }
                }
            }
        }
    }

    /** AudioTrack reports an unsigned 32-bit frame counter. */
    private fun headFrames(): Long = (track?.playbackHeadPosition ?: 0).toLong() and 0xFFFF_FFFFL

    /**
     * The next enqueued packet begins a new agent turn, so per-turn telemetry (first audible
     * sample, mid-turn underruns) restarts from it. Called on turn completion and new user input.
     */
    fun markTurnBoundary() {
        nextEnqueueStartsTurn.set(true)
    }

    /** Non-blocking: AudioTrack is owned exclusively by pcm-playback. */
    fun enqueue(pcm: ByteArray) {
        if (!running || pcm.isEmpty()) return
        val current = queuedBytes.get()
        if (policy.action(current, pcm.size) == PlaybackQueueAction.WARN) {
            metrics.onOverflow()
            Log.w(TAG, "playback backlog=${policy.durationMs(current + pcm.size)}ms; preserving queued speech")
        }

        // Claim the turn-start flag before reading the epoch: a racing flush then either discards
        // nothing we marked or re-arms the flag for the next packet.
        var turnStart = nextEnqueueStartsTurn.getAndSet(false)
        val epoch = queueEpoch.get()
        val writeBytes = sampleRate * GeminiLiveTuning.PLAYBACK_WRITE_CHUNK_MS /
            1_000 * AudioConfig.BYTES_PER_SAMPLE
        // Bound each queued slice to PLAYBACK_WRITE_CHUNK_MS so flush/interrupt can
        // discard remaining work quickly. Small packets get one copy instead of
        // needless copyOfRange slicing.
        if (pcm.size <= writeBytes) {
            queuedBytes.addAndGet(pcm.size)
            queue.offer(QueuedPcm(epoch, pcm.copyOf(), turnStart))
        } else {
            var offset = 0
            while (offset < pcm.size) {
                val end = minOf(offset + writeBytes, pcm.size)
                val size = end - offset
                queuedBytes.addAndGet(size)
                queue.offer(QueuedPcm(epoch, pcm.copyOfRange(offset, end), turnStart))
                turnStart = false
                offset = end
            }
        }

        val pendingMs = queuedDurationMs() + GeminiLiveTuning.OUTPUT_BUFFER_MS +
            GeminiLiveTuning.PLAYBACK_TAIL_GUARD_MS
        AudioPlaybackGate.holdFor(pendingMs)
        metrics.onQueued(queuedDurationMs())
    }

    fun queuedDurationMs(): Int = policy.durationMs(queuedBytes.get())

    /** Immediately discard unsounded speech only on a real provider interruption. */
    fun flush() {
        val epoch = queueEpoch.incrementAndGet()
        queue.clear()
        queuedBytes.set(0)
        nextEnqueueStartsTurn.set(true)
        AudioPlaybackGate.clear()
        // A command is consumed by the audio worker; WebSocket callbacks never
        // wait for a possibly-blocking AudioTrack write to release trackLock.
        if (running) queue.offer(QueuedPcm(epoch, ByteArray(0)))
    }

    fun stop() {
        running = false
        queue.clear()
        queuedBytes.set(0)
        AudioPlaybackGate.clear()
        worker?.interrupt()
        synchronized(trackLock) {
            runCatching {
                track?.pause()
                track?.flush()
                track?.stop()
            }
            track?.release()
            track = null
        }
        worker?.join(500)
        worker = null
    }

    /**
     * Blocking mode should consume the whole slice, but loop defensively on short writes.
     * Returns the number of bytes actually handed to the AudioTrack.
     */
    private fun writeFully(pcm: ByteArray, epoch: Long): Int {
        var offset = 0
        while (running && itemIsCurrent(epoch) && offset < pcm.size) {
            val currentTrack = track ?: return offset
            val written = currentTrack.write(
                pcm,
                offset,
                pcm.size - offset,
                AudioTrack.WRITE_BLOCKING,
            )
            if (written <= 0) {
                Log.e(TAG, "AudioTrack write failed code=$written remaining=${pcm.size - offset}")
                return offset
            }
            offset += written
        }
        return offset
    }

    private fun itemIsCurrent(epoch: Long): Boolean = epoch == queueEpoch.get()

    companion object {
        private const val HEAD_POLL_MS = 10L
    }
}

/**
 * Pure per-turn playback bookkeeping for [PcmPlayer]'s worker, JVM-testable without AudioTrack.
 *
 * - First audible sample: when a turn's first slice is written, the frames already written mark
 *   its start; once the playback head passes that mark the turn is audible.
 * - Underruns: counted only between slices of the same turn. The drain after a turn ends always
 *   underruns, so the counter is re-baselined at each turn start instead of reported.
 */
internal class PlaybackTurnTracker(private val metrics: PlaybackMetrics) {
    private var writtenFrames = 0L
    private var turnStartFrame = -1L
    private var lastUnderruns = 0

    val awaitingHeadAdvance: Boolean get() = turnStartFrame >= 0

    /** After start or a flush: the hardware buffer is empty, so written == played. */
    fun reset(headFrames: Long, underruns: Int) {
        writtenFrames = headFrames
        turnStartFrame = -1L
        lastUnderruns = underruns
    }

    fun beforeWrite(turnStart: Boolean, underruns: Int) {
        if (turnStart) {
            turnStartFrame = writtenFrames
        } else if (underruns > lastUnderruns) {
            metrics.onUnderruns(underruns - lastUnderruns)
        }
        lastUnderruns = underruns
    }

    fun afterWrite(framesWritten: Int, headFrames: Long) {
        writtenFrames += framesWritten.coerceAtLeast(0)
        onHeadPosition(headFrames)
    }

    fun onHeadPosition(headFrames: Long) {
        if (turnStartFrame >= 0 && headFrames > turnStartFrame) {
            turnStartFrame = -1L
            metrics.onPlaybackHeadAdvanced()
        }
    }
}

private const val TAG = "AudioIo"
