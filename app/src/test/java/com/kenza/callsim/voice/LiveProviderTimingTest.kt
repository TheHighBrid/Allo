package com.kenza.callsim.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveProviderTimingTest {
    @Test
    fun recordsConnectionSpeechInterruptAndTeardownSequence() {
        var now = 1_000L
        val timing = LiveProviderTiming("test") { now }

        timing.connectionStarted()
        now = 1_050
        timing.socketConnected()
        now = 1_200
        timing.sessionReady()
        now = 1_500
        timing.speechFinalized()
        now = 1_620
        timing.firstAudio()
        timing.firstAudio() // idempotent
        now = 1_700
        timing.interrupted()
        now = 2_000
        timing.teardownStarted()
        now = 2_040
        timing.teardownComplete()

        val snap = timing.snapshot()
        assertEquals(200L, snap.connectionToReadyMs())
        assertEquals(120L, snap.finalizedToFirstAudioMs())
        assertEquals(40L, snap.teardownDurationMs())
        assertNotNull(snap.interruptedMs)
        assertTrue(snap.firstAudioMs == 1_620L)
    }
}

class LiveTurnTelemetryLocalSpeechTest {
    @Test
    fun fillsLocalSpeechStartAndEndOnCurrentTurn() {
        val telemetry = LiveTurnTelemetry(enabled = false)
        telemetry.localSpeechStart(10)
        telemetry.localSpeechEnd(40)
        telemetry.micQueued(12)
        val metrics = telemetry.currentMetrics()
        assertEquals(10L, metrics.localSpeechStartMs)
        assertEquals(40L, metrics.localSpeechEndMs)
        assertEquals(12L, metrics.socketQueuePeakBytes)
        // first start wins
        telemetry.localSpeechStart(99)
        assertEquals(10L, telemetry.currentMetrics().localSpeechStartMs)
    }

    @Test
    fun serializedMetricStillHasNoContentFields() {
        val text = LiveTurnTelemetry(false).toJson(
            LiveTurnMetrics(1, localSpeechStartMs = 1, localSpeechEndMs = 2),
        )
        assertTrue(!text.contains("transcript", ignoreCase = true))
        assertNull(Regex("\"pcm\"|\"audioBase64\"|\"user_text\"", RegexOption.IGNORE_CASE).find(text))
    }
}
