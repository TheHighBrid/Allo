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
import kotlin.concurrent.thread
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicInteger

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
                    if (!muted && (!agentSpeaking || fullDuplex)) {
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

/** Streams agent PCM to the phone audio route with a deliberately short queue. */
interface PlaybackMetrics {
    fun onQueued(queueMs: Int) {}
    fun onOverflow() {}
    fun onPlaybackHeadAdvanced() {}
    companion object { val NOOP = object : PlaybackMetrics {} }
}

class PcmPlayer(
    private val sampleRate: Int = AudioConfig.SAMPLE_RATE,
    private val metrics: PlaybackMetrics = PlaybackMetrics.NOOP,
) {

    private var track: AudioTrack? = null
    private data class QueuedPcm(val epoch: Long, val pcm: ByteArray)

    private val queue = LinkedBlockingQueue<QueuedPcm>()
    private val queuedBytes = AtomicInteger()
    private val queueEpoch = java.util.concurrent.atomic.AtomicLong()
    private val trackLock = Any()
    private val policy = PlaybackQueuePolicy(sampleRate)
    @Volatile private var running = false
    private var worker: Thread? = null

    fun start() {
        if (track != null) return
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
        Log.i(TAG, "AudioTrack buffer frames requested=$requestedFrames actual=${track?.bufferSizeInFrames} capacity=${track?.bufferCapacityInFrames} resized=$resized underruns=${track?.underrunCount}")
        running = true
        worker = thread(name = "pcm-playback", priority = Thread.MAX_PRIORITY) {
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
            var advanced = false
            while (running) {
                val item = runCatching { queue.take() }.getOrNull() ?: continue
                val pcm = item.pcm
                if (pcm.isEmpty()) {
                    synchronized(trackLock) {
                        runCatching { track?.pause(); track?.flush(); track?.play() }
                    }
                    continue
                }
                val writeBytes = sampleRate * GeminiLiveTuning.PLAYBACK_WRITE_CHUNK_MS /
                    1_000 * AudioConfig.BYTES_PER_SAMPLE
                var offset = 0
                while (running && offset < pcm.size && item.epoch == queueEpoch.get()) {
                    val size = minOf(writeBytes, pcm.size - offset)
                    synchronized(trackLock) {
                        if (item.epoch == queueEpoch.get()) {
                            track?.write(pcm, offset, size, AudioTrack.WRITE_BLOCKING)
                        }
                    }
                    offset += size
                }
                if (item.epoch == queueEpoch.get()) {
                    queuedBytes.updateAndGet { (it - pcm.size).coerceAtLeast(0) }
                }
                if (!advanced && (track?.playbackHeadPosition ?: 0) > 0) {
                    advanced = true
                    metrics.onPlaybackHeadAdvanced()
                }
            }
        }
    }

    /** Non-blocking: AudioTrack is owned exclusively by pcm-playback. */
    fun enqueue(pcm: ByteArray) {
        if (!running || pcm.isEmpty()) return
        val current = queuedBytes.get()
        if (!policy.accepts(current, pcm.size)) {
            metrics.onOverflow()
        }
        // A normal 400 ms burst is only reported, never discarded. Clearing at
        // that threshold produced half-words whenever network delivery briefly
        // ran ahead of AudioTrack. Resync only if audio is several seconds stale.
        if (policy.exceedsHardLimit(current, pcm.size)) flush()

        val epoch = queueEpoch.get()
        // Copy once and return. Chunking belongs on the audio worker; doing many
        // copyOfRange calls here made large model frames stall the socket callback.
        queuedBytes.addAndGet(pcm.size)
        queue.offer(QueuedPcm(epoch, pcm.copyOf()))
        metrics.onQueued(queuedDurationMs())
    }

    fun queuedDurationMs(): Int = policy.durationMs(queuedBytes.get())

    /** Immediately discard unsounded speech when Gemini reports interruption. */
    fun flush() {
        val epoch = queueEpoch.incrementAndGet()
        queue.clear()
        queuedBytes.set(0)
        // A command is consumed by the audio worker; WebSocket callbacks never
        // wait for a possibly-blocking AudioTrack write to release trackLock.
        if (running) queue.offer(QueuedPcm(epoch, ByteArray(0)))
    }

    fun stop() {
        running = false
        queue.clear()
        queuedBytes.set(0)
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

    companion object
}

private const val TAG = "AudioIo"
