package com.kenza.callsim.voice

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioPlaybackGateTest {
    @Test fun gateStaysClosedForPendingPlaybackAndCanBeClearedOnInterruption() {
        AudioPlaybackGate.clear()
        assertFalse(AudioPlaybackGate.isBlocked())

        AudioPlaybackGate.holdFor(1_000)
        assertTrue(AudioPlaybackGate.isBlocked())

        AudioPlaybackGate.clear()
        assertFalse(AudioPlaybackGate.isBlocked())
    }
}
