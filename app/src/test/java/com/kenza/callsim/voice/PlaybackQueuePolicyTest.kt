package com.kenza.callsim.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackQueuePolicyTest {
    private val policy = PlaybackQueuePolicy(sampleRate = 24_000, maximumMs = 400)

    @Test fun durationUsesPcm16MonoByteRate() {
        assertEquals(100, policy.durationMs(4_800))
    }

    @Test fun rejectsAudioBeyondBoundedQueue() {
        assertTrue(policy.accepts(0, 19_200))
        assertFalse(policy.accepts(19_200, 1))
    }

    @Test fun hardLimitAllowsJitterWithoutAllowingSecondsOfStaleAudio() {
        assertFalse(policy.exceedsHardLimit(19_200, 4_800))
        assertTrue(policy.exceedsHardLimit(192_000, 1))
    }
}
