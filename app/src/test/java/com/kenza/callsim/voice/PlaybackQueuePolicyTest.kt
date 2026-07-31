package com.kenza.callsim.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PlaybackQueuePolicyTest {
    private val policy = PlaybackQueuePolicy(sampleRate = 24_000, warningMs = 400)

    @Test fun durationUsesPcm16MonoByteRate() {
        assertEquals(100, policy.durationMs(4_800))
    }

    @Test fun softLimitWarnsWithoutAuthorizingAudioDeletion() {
        assertEquals(PlaybackQueueAction.ACCEPT, policy.action(0, 19_200))
        assertEquals(PlaybackQueueAction.WARN, policy.action(19_200, 1))
        assertFalse(policy.accepts(19_200, 1))
    }

    @Test fun longRepliesRemainWarningsEvenWithLargePlaybackBacklogs() {
        val fourSeconds = 24_000 * AudioConfig.BYTES_PER_SAMPLE * 4
        val sixtySeconds = 24_000 * AudioConfig.BYTES_PER_SAMPLE * 60

        assertEquals(PlaybackQueueAction.WARN, policy.action(0, fourSeconds))
        assertEquals(PlaybackQueueAction.WARN, policy.action(fourSeconds, sixtySeconds))
    }
}
