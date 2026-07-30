package com.kenza.callsim.voice

import android.util.Log

data class LiveTurnMetrics(
    val turnId: Long,
    val localSpeechStartMs: Long? = null,
    val localSpeechEndMs: Long? = null,
    val lastMicPacketQueuedMs: Long? = null,    val firstModelPacketReceivedMs: Long? = null,
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
    private var nextId = 1L
    private var current = LiveTurnMetrics(nextId)

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

    /** Pure JVM-safe serialization so local unit tests do not depend on Android's JSONObject stub. */
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
