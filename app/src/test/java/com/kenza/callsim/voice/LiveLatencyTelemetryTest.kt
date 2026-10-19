package com.kenza.callsim.voice

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class LiveLatencyTelemetryTest {

    private var now = 0L
    private fun telemetry() = LiveTurnTelemetry(enabled = false, clockMs = { now })

    @Test
    fun replyLatencyRunsFromSpeechEndToFirstAudibleSample() {
        val t = telemetry()
        t.localSpeechStart(atMs = 100)
        t.localSpeechEnd(atMs = 1_000)
        now = 1_400; t.modelAudio()
        now = 1_450; t.playback.onQueued(queueMs = 40)
        now = 1_520; t.playback.onPlaybackHeadAdvanced()
        val m = t.currentMetrics()
        assertEquals(520L, m.speechEndToPlaybackMs())
        assertEquals(400L, m.speechEndToFirstPacketMs())
    }

    @Test
    fun speechWhileKenzaIsAudibleIsBargeInAndDoesNotMoveSpeechEnd() {
        val t = telemetry()
        t.localSpeechStart(atMs = 0)
        t.localSpeechEnd(atMs = 500)
        now = 900; t.playback.onPlaybackHeadAdvanced()
        t.localSpeechStart(atMs = 1_200)
        t.localSpeechEnd(atMs = 1_900)
        now = 1_450; t.interrupted()
        now = 1_480; t.playback.onPlaybackStopped()
        val m = t.currentMetrics()
        assertEquals(500L, m.localSpeechEndMs)
        assertEquals(250L, m.bargeInToInterruptMs())
        assertEquals(30L, m.interruptToPlaybackStopMs())
    }

    @Test
    fun playbackStopWithoutInterruptionIsIgnored() {
        val t = telemetry()
        now = 10; t.playback.onPlaybackStopped()
        assertNull(t.currentMetrics().playbackStoppedMs)
        assertNull(LiveTurnMetrics(1, localSpeechEndMs = 50, firstPlaybackHeadAdvanceMs = 40).speechEndToPlaybackMs())
    }

    @Test
    fun underrunsAccumulatePerTurnAndResetOnComplete() {
        val t = telemetry()
        t.playback.onUnderruns(2)
        t.playback.onUnderruns(0)
        t.playback.onUnderruns(1)
        assertEquals(3, t.currentMetrics().underrunsDelta)
        t.complete(route = ROUTE, sessionAgeMs = 0)
        assertEquals(0, t.currentMetrics().underrunsDelta)
    }

    @Test
    fun nearestRankPercentiles() {
        val values = listOf(500L, 100L, 300L, 200L, 400L)
        assertEquals(300L, LatencyStats.percentile(values, 50))
        assertEquals(500L, LatencyStats.percentile(values, 95))
        assertEquals(100L, LatencyStats.percentile(values, 1))
        assertNull(LatencyStats.percentile(emptyList(), 50))
    }

    @Test
    fun sessionSummaryIsValidContentFreeJson() {
        val t = telemetry()
        listOf(300L, 500L, 700L).forEachIndexed { index, latency ->
            val base = index * 10_000L
            t.localSpeechEnd(atMs = base)
            now = base + latency; t.playback.onPlaybackHeadAdvanced()
            t.playback.onUnderruns(1)
            t.complete(route = ROUTE, sessionAgeMs = base)
        }
        val timing = LiveProviderTimingSnapshot(
            connectionStartedMs = 0,
            sessionReadyMs = 800,
            teardownStartedMs = 30_000,
            teardownCompleteMs = 30_040,
        )
        val text = t.session.summary(provider = "eleven\"labs", timing = timing)
        val json = JSONObject(text)
        assertEquals("elevenlabs", json.getString("provider"))
        assertEquals(3, json.getInt("turns"))
        assertEquals(3, json.getInt("underruns"))
        assertEquals(300L, json.getLong("startToFirstPlaybackMs"))
        assertEquals(800L, json.getLong("connectionToReadyMs"))
        assertEquals(40L, json.getLong("teardownMs"))
        val reply = json.getJSONObject("speechEndToPlaybackMs")
        assertEquals(500L, reply.getLong("p50"))
        assertEquals(700L, reply.getLong("p95"))
        assertFalse(text.contains("transcript", ignoreCase = true))
        assertFalse(text.contains("audio", ignoreCase = true))
    }

    @Test
    fun perTurnJsonStaysContentFree() {
        val text = LiveTurnJson.encode(LiveTurnMetrics(1, localSpeechEndMs = 1, firstPlaybackHeadAdvanceMs = 2))
        assertEquals(1L, JSONObject(text).getLong("speechEndToPlaybackMs"))
        assertFalse(text.contains("audio", ignoreCase = true))
    }

    private companion object {
        const val ROUTE = "earpiece_or_headset"
    }
}
