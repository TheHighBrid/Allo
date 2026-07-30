package com.kenza.callsim.voice

import android.util.Log
import org.json.JSONObject

data class LiveTurnMetrics(
    val turnId: Long,
    val localSpeechStartMs: Long? = null,
    val localSpeechEndMs: Long? = null,
    val lastMicPacketQueuedMs: Long? = null,
    val firstModelAudioReceivedMs: Long? = null,
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

/** Content-free per-turn state. No method accepts transcript or audio payloads. */
class LiveTurnTelemetry(private val enabled: Boolean) : PlaybackMetrics {
    private var nextId = 1L
    private var current = LiveTurnMetrics(nextId)

    @Synchronized fun micQueued(socketBytes: Long) {
        val now = System.currentTimeMillis()
        current = current.copy(lastMicPacketQueuedMs = now,
            socketQueuePeakBytes = maxOf(current.socketQueuePeakBytes, socketBytes))
    }
    @Synchronized fun modelAudio() { if (current.firstModelAudioReceivedMs == null) current = current.copy(firstModelAudioReceivedMs = System.currentTimeMillis()) }
    @Synchronized override fun onQueued(queueMs: Int) {
        current = current.copy(firstPlaybackQueuedMs = current.firstPlaybackQueuedMs ?: System.currentTimeMillis(), playbackQueuePeakMs = maxOf(current.playbackQueuePeakMs, queueMs))
    }
    @Synchronized override fun onPlaybackHeadAdvanced() { if (current.firstPlaybackHeadAdvanceMs == null) current = current.copy(firstPlaybackHeadAdvanceMs = System.currentTimeMillis()) }
    @Synchronized fun usage(total: Int?) { current = current.copy(totalTokenCount = total) }
    @Synchronized fun generationComplete() { current = current.copy(generationCompleteMs = System.currentTimeMillis()) }
    @Synchronized fun interrupted() { current = current.copy(interrupted = true) }
    @Synchronized fun complete(route: String, sessionAgeMs: Long) {
        val done = current.copy(turnCompleteMs = System.currentTimeMillis(), route = route, sessionAgeMs = sessionAgeMs)
        if (enabled) Log.i("LiveTurnMetrics", toJson(done).toString())
        current = LiveTurnMetrics(++nextId)
    }
    fun toJson(m: LiveTurnMetrics): JSONObject = JSONObject().apply {
        put("turnId", m.turnId); put("localSpeechStartMs", m.localSpeechStartMs)
        put("localSpeechEndMs", m.localSpeechEndMs); put("lastMicPacketQueuedMs", m.lastMicPacketQueuedMs)
        put("firstModelAudioReceivedMs", m.firstModelAudioReceivedMs); put("firstPlaybackQueuedMs", m.firstPlaybackQueuedMs)
        put("firstPlaybackHeadAdvanceMs", m.firstPlaybackHeadAdvanceMs); put("generationCompleteMs", m.generationCompleteMs)
        put("turnCompleteMs", m.turnCompleteMs); put("interrupted", m.interrupted)
        put("socketQueuePeakBytes", m.socketQueuePeakBytes); put("playbackQueuePeakMs", m.playbackQueuePeakMs)
        put("underrunsDelta", m.underrunsDelta); put("totalTokenCount", m.totalTokenCount)
        put("route", m.route); put("networkClass", m.networkClass); put("sessionAgeMs", m.sessionAgeMs)
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
        // Qualify the receiver: unqualified clear() clears this temporary list,
        // not the transcript buffers, causing old turns to be committed again.
        this@TranscriptAssembler.clear()
    }
    fun clear() { user.clear(); agent.clear() }
    private fun append(target: StringBuilder, raw: String) {
        val text = raw.trim(); if (text.isEmpty()) return
        val existing = target.toString()
        if (existing.endsWith(text)) return
        var overlap = minOf(existing.length, text.length)
        while (overlap > 0 && !existing.endsWith(text.take(overlap))) overlap--
        target.append(text.drop(overlap))
    }
}
