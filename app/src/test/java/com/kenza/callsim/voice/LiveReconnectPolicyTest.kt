package com.kenza.callsim.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveReconnectPolicyTest {
    @Test fun defersUntilAResumableQuietBoundaryOrDeadline() {
        assertFalse(LiveReconnectPolicy.canRotate(true, true, true, false, false))
        assertFalse(LiveReconnectPolicy.canRotate(true, true, false, true, false))
        assertFalse(LiveReconnectPolicy.canRotate(true, false, false, false, false))
        assertTrue(LiveReconnectPolicy.canRotate(true, true, false, false, false))
        assertTrue(LiveReconnectPolicy.canRotate(true, false, true, true, true))
        assertFalse(LiveReconnectPolicy.canRotate(false, true, false, false, true))
    }

    @Test fun backoffAndDeadlineAreBounded() {
        assertEquals(0L, LiveReconnectPolicy.retryDelayMs(1, planned = true))
        assertEquals(listOf(250L, 500L, 1000L, 2000L),
            (1..4).map { LiveReconnectPolicy.retryDelayMs(it, planned = false) })
        assertEquals(3500L, LiveReconnectPolicy.goAwayDelayMs("4s"))
        assertEquals(0L, LiveReconnectPolicy.goAwayDelayMs("0.1s"))
        assertEquals(60_000L, LiveReconnectPolicy.goAwayDelayMs("100s"))
    }

    @Test fun recordsTwoIndependentResumptionsAndSpeechLoss() {
        var now = 1000L
        val timing = LiveProviderTiming("test") { now }
        timing.connectionStarted()
        timing.goAway()
        timing.reconnectStarted(false)
        now = 1500; timing.reconnectReady()
        now = 1600; timing.firstAudio()
        now = 2000; timing.reconnectStarted(true)
        timing.droppedSpeechChunk()
        now = 2250; timing.reconnectReady()
        now = 2400; timing.firstAudio()
        val snap = timing.snapshot()
        assertEquals(2, snap.reconnectSuccesses)
        assertEquals(1, snap.possiblyDroppedTurns)
        assertEquals(1, snap.droppedSpeechChunks)
        assertEquals(250L, snap.lastReconnectReadyMs)
        assertEquals(400L, snap.reconnectToFirstAudioMs)
    }
}
