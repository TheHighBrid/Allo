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
    val bargeInStartMs: Long? = null,
    val interruptionReceivedMs: Long? = null,
    val playbackStoppedMs: Long? = null,
) {
    /** Local end of speech to Kenza's first audible sample: the latency the caller feels. */
    fun speechEndToPlaybackMs(): Long? = span(localSpeechEndMs, firstPlaybackHeadAdvanceMs)

    /** Local end of speech to the first reply packet from the provider (network + server time). */
    fun speechEndToFirstPacketMs(): Long? = span(localSpeechEndMs, firstModelPacketReceivedMs)

    /** Caller starts talking over Kenza until the provider confirms the interruption. */
    fun bargeInToInterruptMs(): Long? = span(bargeInStartMs, interruptionReceivedMs)

    /** Provider interruption until the AudioTrack has actually been silenced. */
    fun interruptToPlaybackStopMs(): Long? = span(interruptionReceivedMs, playbackStoppedMs)

    private fun span(start: Long?, end: Long?): Long? =
        if (start == null || end == null || end < start) null else end - start
}

/** Nearest-rank percentiles over completed turns; content-free by construction. */
internal object LatencyStats {
    fun percentile(values: List<Long>, p: Int): Long? {
        if (values.isEmpty()) return null
        val sorted = values.sorted()
        val rank = kotlin.math.ceil(p / 100.0 * sorted.size).toInt().coerceIn(1, sorted.size)
        return sorted[rank - 1]
    }

    fun summarize(name: String, values: List<Long>): String =
        "\"$name\":{\"n\":${values.size},\"p50\":${percentile(values, 50)},\"p95\":${percentile(values, 95)}}"
}

/** Content-free per-turn state. No method accepts transcript or media payloads. */
class LiveTurnTelemetry(
    private val enabled: Boolean,
    private val clockMs: () -> Long = { System.currentTimeMillis() },
) : PlaybackMetrics {
    init {
        LiveDiagnosticsGate.enabled = enabled
    }

    private var nextId = 1L
    private var current = LiveTurnMetrics(nextId)
    private val sessionStartedMs = clockMs()
    private var sessionFirstPlaybackMs: Long? = null
    private val completedTurns = mutableListOf<LiveTurnMetrics>()

    @Synchronized fun localSpeechStart(atMs: Long = clockMs()) {
        if (current.localSpeechStartMs == null) {
            current = current.copy(localSpeechStartMs = atMs)
        }
        // Speech that starts while Kenza is already audible is a barge-in attempt.
        if (current.firstPlaybackHeadAdvanceMs != null && current.bargeInStartMs == null && !current.interrupted) {
            current = current.copy(bargeInStartMs = atMs)
        }
    }

    /**
     * Only speech that ends before Kenza becomes audible counts as the turn's end of speech;
     * later local speech (talking over her) must not distort the reply latency.
     */
    @Synchronized fun localSpeechEnd(atMs: Long = clockMs()) {
        if (current.firstPlaybackHeadAdvanceMs == null) {
            current = current.copy(localSpeechEndMs = atMs)
        }
    }

    @Synchronized fun noteUnderruns(delta: Int) {
        if (delta <= 0) return
        current = current.copy(underrunsDelta = current.underrunsDelta + delta)
    }

    override fun onUnderruns(delta: Int) = noteUnderruns(delta)

    @Synchronized override fun onPlaybackStopped() {
        if (current.interruptionReceivedMs != null && current.playbackStoppedMs == null) {
            current = current.copy(playbackStoppedMs = clockMs())
        }
    }

    @Synchronized fun currentMetrics(): LiveTurnMetrics = current

    @Synchronized fun micQueued(socketBytes: Long) {
        val now = clockMs()
        current = current.copy(
            lastMicPacketQueuedMs = now,
            socketQueuePeakBytes = maxOf(current.socketQueuePeakBytes, socketBytes),
        )
    }

    @Synchronized fun modelAudio() {
        if (current.firstModelPacketReceivedMs == null) {
            current = current.copy(firstModelPacketReceivedMs = clockMs())
        }
    }

    @Synchronized override fun onQueued(queueMs: Int) {
        current = current.copy(
            firstPlaybackQueuedMs = current.firstPlaybackQueuedMs ?: clockMs(),
            playbackQueuePeakMs = maxOf(current.playbackQueuePeakMs, queueMs),
        )
    }

    @Synchronized override fun onPlaybackHeadAdvanced() {
        val now = clockMs()
        if (sessionFirstPlaybackMs == null) sessionFirstPlaybackMs = now
        if (current.firstPlaybackHeadAdvanceMs == null) {
            current = current.copy(firstPlaybackHeadAdvanceMs = now)
        }
    }

    @Synchronized fun usage(total: Int?) {
        current = current.copy(totalTokenCount = total)
    }

    @Synchronized fun generationComplete() {
        current = current.copy(generationCompleteMs = clockMs())
    }

    @Synchronized fun interrupted() {
        current = current.copy(
            interrupted = true,
            interruptionReceivedMs = current.interruptionReceivedMs ?: clockMs(),
        )
    }

    @Synchronized fun complete(route: String, sessionAgeMs: Long) {
        val done = current.copy(
            turnCompleteMs = clockMs(),
            route = route,
            sessionAgeMs = sessionAgeMs,
        )
        if (enabled) Log.i("LiveTurnMetrics", toJson(done))
        completedTurns += done
        if (completedTurns.size > MAX_SUMMARY_TURNS) completedTurns.removeAt(0)
        current = LiveTurnMetrics(++nextId)
    }

    /**
     * Content-free per-session latency summary (P50/P95 over completed turns) plus the provider's
     * connection/teardown timings. Returned for tests; only logged when diagnostics are enabled.
     */
    @Synchronized fun sessionSummary(provider: String, timing: LiveProviderTimingSnapshot?): String {
        val turns = completedTurns.toList()
        val startToFirstPlayback = sessionFirstPlaybackMs?.let { it - sessionStartedMs }
        return buildString {
            append("{\"provider\":\"").append(provider.filter { it.isLetterOrDigit() || it == '_' }).append("\"")
            append(",\"turns\":").append(turns.size)
            append(",\"interruptedTurns\":").append(turns.count { it.interrupted })
            append(",\"underruns\":").append(turns.sumOf { it.underrunsDelta })
            append(",\"startToFirstPlaybackMs\":").append(startToFirstPlayback)
            append(",\"connectionToReadyMs\":").append(timing?.connectionToReadyMs())
            append(",\"teardownMs\":").append(timing?.teardownDurationMs())
            val spans = listOf<Pair<String, (LiveTurnMetrics) -> Long?>>(
                "speechEndToPlaybackMs" to LiveTurnMetrics::speechEndToPlaybackMs,
                "speechEndToFirstPacketMs" to LiveTurnMetrics::speechEndToFirstPacketMs,
                "bargeInToInterruptMs" to LiveTurnMetrics::bargeInToInterruptMs,
                "interruptToPlaybackStopMs" to LiveTurnMetrics::interruptToPlaybackStopMs,
            )
            spans.forEach { (name, span) ->
                append(',').append(LatencyStats.summarize(name, turns.mapNotNull(span)))
            }
            append('}')
        }
    }

    fun logSessionSummary(provider: String, timing: LiveProviderTimingSnapshot?) {
        if (enabled) Log.i("LiveSessionMetrics", sessionSummary(provider, timing))
    }

    private companion object {
        const val MAX_SUMMARY_TURNS = 500
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
            "bargeInStartMs" to m.bargeInStartMs,
            "interruptionReceivedMs" to m.interruptionReceivedMs,
            "playbackStoppedMs" to m.playbackStoppedMs,
            "speechEndToPlaybackMs" to m.speechEndToPlaybackMs(),
            "speechEndToFirstPacketMs" to m.speechEndToFirstPacketMs(),
            "bargeInToInterruptMs" to m.bargeInToInterruptMs(),
            "interruptToPlaybackStopMs" to m.interruptToPlaybackStopMs(),
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

    /** Applies a provider correction to not-yet-committed agent text. Returns true when applied. */
    fun correctAgent(original: String, corrected: String): Boolean {
        val fixed = replaceLast(agent.toString(), original, corrected) ?: return false
        agent.clear()
        agent.append(fixed)
        return true
    }

    companion object {
        /**
         * Applies a provider correction to the most recent committed agent line that contains the
         * original reply (interruptions commit the turn before the correction arrives).
         */
        fun correctCommitted(
            entries: MutableList<Pair<String, String>>,
            original: String,
            corrected: String,
        ): Boolean {
            for (index in entries.indices.reversed()) {
                val (role, text) = entries[index]
                if (role != "agent") continue
                val fixed = replaceLast(text, original, corrected) ?: continue
                if (fixed.isBlank()) entries.removeAt(index) else entries[index] = role to fixed
                return true
            }
            return false
        }

        internal fun replaceLast(text: String, original: String, corrected: String): String? {
            val needle = original.trim()
            if (needle.isEmpty()) return null
            val at = text.lastIndexOf(needle)
            if (at < 0) return null
            return (text.substring(0, at) + corrected.trim() + text.substring(at + needle.length))
                .replace(Regex("\\s+"), " ")
                .trim()
        }
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
