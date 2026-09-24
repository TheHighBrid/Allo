package com.kenza.callsim.voice

import android.util.Log

/** Process-local diagnostics gate. CallViewModel refreshes it from the user's setting each session. */
object LiveDiagnosticsGate {
    @Volatile var enabled: Boolean = false
}

data class LiveProviderTimingSnapshot(
    val connectionStartedMs: Long? = null,
    val socketConnectedMs: Long? = null,
    val sessionReadyMs: Long? = null,
    val speechFinalizedMs: Long? = null,
    val firstAudioMs: Long? = null,
    val interruptedMs: Long? = null,
    val teardownStartedMs: Long? = null,
    val teardownCompleteMs: Long? = null,
) {
    fun connectionToReadyMs(): Long? = duration(connectionStartedMs, sessionReadyMs)
    fun finalizedToFirstAudioMs(): Long? = duration(speechFinalizedMs, firstAudioMs)
    fun teardownDurationMs(): Long? = duration(teardownStartedMs, teardownCompleteMs)

    private fun duration(start: Long?, end: Long?): Long? =
        if (start == null || end == null || end < start) null else end - start
}

/**
 * Content-free lifecycle instrumentation shared by live providers.
 * Event timestamps stay in memory regardless of logging, but Logcat emission follows the existing
 * user-controlled diagnostics opt-in through [LiveDiagnosticsGate].
 */
class LiveProviderTiming(
    private val tag: String,
    private val clockMs: () -> Long = { System.currentTimeMillis() },
) {
    @Volatile private var state = LiveProviderTimingSnapshot()
    @Volatile private var firstAudioRecorded = false

    @Synchronized fun connectionStarted() {
        firstAudioRecorded = false
        state = LiveProviderTimingSnapshot(connectionStartedMs = clockMs())
        emit("connection_started")
    }

    @Synchronized fun socketConnected() {
        state = state.copy(socketConnectedMs = state.socketConnectedMs ?: clockMs())
        emit("socket_connected")
    }

    @Synchronized fun sessionReady() {
        state = state.copy(sessionReadyMs = state.sessionReadyMs ?: clockMs())
        emit("session_ready")
    }

    @Synchronized fun speechFinalized() {
        state = state.copy(speechFinalizedMs = clockMs(), firstAudioMs = null, interruptedMs = null)
        firstAudioRecorded = false
        emit("speech_finalized")
    }

    @Synchronized fun firstAudio() {
        if (firstAudioRecorded) return
        firstAudioRecorded = true
        state = state.copy(firstAudioMs = clockMs())
        emit("first_audio")
    }

    @Synchronized fun interrupted() {
        state = state.copy(interruptedMs = clockMs())
        emit("interrupted")
    }

    @Synchronized fun teardownStarted() {
        state = state.copy(teardownStartedMs = clockMs(), teardownCompleteMs = null)
        emit("teardown_started")
    }

    @Synchronized fun teardownComplete() {
        state = state.copy(teardownCompleteMs = clockMs())
        emit("teardown_complete")
    }

    @Synchronized fun snapshot(): LiveProviderTimingSnapshot = state

    private fun emit(event: String) {
        if (!LiveDiagnosticsGate.enabled) return
        val current = state
        Log.i(
            "LiveProviderTiming",
            "provider=$tag event=$event connectionToReadyMs=${current.connectionToReadyMs()} " +
                "finalizedToFirstAudioMs=${current.finalizedToFirstAudioMs()} " +
                "teardownMs=${current.teardownDurationMs()}",
        )
    }
}

data class LiveTurnMetrics(
    val turnId: Long,
    val localSpeechStartMs: Long? = null,
    val localSpeechEndMs: Long? = null,
    val lastMicPacketQueuedMs: Long? = null,
    val firstModelPacketReceivedMs: Long? = null,
    val firstPlaybackQueuedMs: Long? = null,
    val firstPlaybackHeadAdvanceMs: Long? = null,
    val generationCompleteMs: Long? = null,
    val turnCompleteMs: Long? = null,
    val interrupted: Boolean = false,
    val socketQueuePeakBytes: Long = 0,
    val playbackQueuePeakMs: Int = 0,
    val underrunsDelta: Int = 0,
    val totalTokenCount: Int? = null,
    val route: String = "unknown",
    val networkClass: String = "unknown",
    val sessionAgeMs: Long = 0,
)

/** Content-free per-turn state. No method accepts transcript or media payloads. */
class LiveTurnTelemetry(private val enabled: Boolean) : PlaybackMetrics {
    init {
        LiveDiagnosticsGate.enabled = enabled
    }

    private var nextId = 1L
    private var current = LiveTurnMetrics(nextId)

    @Synchronized fun localSpeechStart(atMs: Long = System.currentTimeMillis()) {
        if (current.localSpeechStartMs == null) {
            current = current.copy(localSpeechStartMs = atMs)
        }
    }

    @Synchronized fun localSpeechEnd(atMs: Long = System.currentTimeMillis()) {
        current = current.copy(localSpeechEndMs = atMs)
    }

    @Synchronized fun noteUnderruns(delta: Int) {
        if (delta <= 0) return
        current = current.copy(underrunsDelta = current.underrunsDelta + delta)
    }

    @Synchronized fun currentMetrics(): LiveTurnMetrics = current

    @Synchronized fun micQueued(socketBytes: Long) {
        val now = System.currentTimeMillis()
        current = current.copy(
            lastMicPacketQueuedMs = now,
            socketQueuePeakBytes = maxOf(current.socketQueuePeakBytes, socketBytes),
        )
    }

    @Synchronized fun modelAudio() {
        if (current.firstModelPacketReceivedMs == null) {
            current = current.copy(firstModelPacketReceivedMs = System.currentTimeMillis())
        }
    }

    @Synchronized override fun onQueued(queueMs: Int) {
        current = current.copy(
            firstPlaybackQueuedMs = current.firstPlaybackQueuedMs ?: System.currentTimeMillis(),
            playbackQueuePeakMs = maxOf(current.playbackQueuePeakMs, queueMs),
        )
    }

    @Synchronized override fun onPlaybackHeadAdvanced() {
        if (current.firstPlaybackHeadAdvanceMs == null) {
            current = current.copy(firstPlaybackHeadAdvanceMs = System.currentTimeMillis())
        }
    }

    @Synchronized fun usage(total: Int?) {
        current = current.copy(totalTokenCount = total)
    }

    @Synchronized fun generationComplete() {
        current = current.copy(generationCompleteMs = System.currentTimeMillis())
    }

    @Synchronized fun interrupted() {
        current = current.copy(interrupted = true)
    }

    @Synchronized fun complete(route: String, sessionAgeMs: Long) {
        val done = current.copy(
            turnCompleteMs = System.currentTimeMillis(),
            route = route,
            sessionAgeMs = sessionAgeMs,
        )
        if (enabled) Log.i("LiveTurnMetrics", toJson(done))
        current = LiveTurnMetrics(++nextId)
    }

    fun toJson(m: LiveTurnMetrics): String = buildString {
        val fields = listOf<Pair<String, Any?>>(
            "turnId" to m.turnId,
            "localSpeechStartMs" to m.localSpeechStartMs,
            "localSpeechEndMs" to m.localSpeechEndMs,
            "lastMicPacketQueuedMs" to m.lastMicPacketQueuedMs,
            "firstModelPacketReceivedMs" to m.firstModelPacketReceivedMs,
            "firstPlaybackQueuedMs" to m.firstPlaybackQueuedMs,
            "firstPlaybackHeadAdvanceMs" to m.firstPlaybackHeadAdvanceMs,
            "generationCompleteMs" to m.generationCompleteMs,
            "turnCompleteMs" to m.turnCompleteMs,
            "interrupted" to m.interrupted,
            "socketQueuePeakBytes" to m.socketQueuePeakBytes,
            "playbackQueuePeakMs" to m.playbackQueuePeakMs,
            "underrunsDelta" to m.underrunsDelta,
            "totalTokenCount" to m.totalTokenCount,
            "route" to m.route,
            "networkClass" to m.networkClass,
            "sessionAgeMs" to m.sessionAgeMs,
        )

        append('{')
        fields.forEachIndexed { index, (name, value) ->
            if (index > 0) append(',')
            append('"').append(name).append("\":")
            appendJsonValue(value)
        }
        append('}')
    }

    private fun StringBuilder.appendJsonValue(value: Any?) {
        when (value) {
            null -> append("null")
            is Number, is Boolean -> append(value.toString())
            else -> {
                append('"')
                value.toString().forEach { character ->
                    when (character) {
                        '\\' -> append("\\\\")
                        '"' -> append("\\\"")
                        '\b' -> append("\\b")
                        '\u000C' -> append("\\f")
                        '\n' -> append("\\n")
                        '\r' -> append("\\r")
                        '\t' -> append("\\t")
                        else -> {
                            if (character.code < 0x20) {
                                append("\\u")
                                append(character.code.toString(16).padStart(4, '0'))
                            } else {
                                append(character)
                            }
                        }
                    }
                }
                append('"')
            }
        }
    }
}

class TranscriptAssembler {
    private val user = StringBuilder()
    private val agent = StringBuilder()

    fun appendUser(fragment: String) = append(user, fragment)
    fun appendAgent(fragment: String) = append(agent, fragment)

    fun commit(): List<Pair<String, String>> = buildList {
        user.toString().trim().takeIf(String::isNotEmpty)?.let { add("user" to it) }
        agent.toString().trim().takeIf(String::isNotEmpty)?.let { add("agent" to it) }
        this@TranscriptAssembler.clear()
    }

    fun clear() {
        user.clear()
        agent.clear()
    }

    private fun append(target: StringBuilder, raw: String) {
        val text = raw.trim()
        if (text.isEmpty()) return
        val existing = target.toString()
        if (existing.endsWith(text)) return
        var overlap = minOf(existing.length, text.length)
        while (overlap > 0 && !existing.endsWith(text.take(overlap))) overlap--
        target.append(text.drop(overlap))
    }
}
