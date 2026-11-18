package com.kenza.callsim.voice

import org.junit.Assert.*
import org.junit.Test

class AdaptiveTurnPolicyTest {
    private val voiced = ByteArray(1280).also { for (i in it.indices step 2) it[i + 1] = 8 }
    private val quiet = ByteArray(1280)

    @Test fun contextualPauseOrdering() {
        assertEquals(TurnPolicy.Intent.QUICK, TurnPolicy.classify("Are you there?"))
        assertEquals(TurnPolicy.Intent.THINKING, TurnPolicy.classify("I mean..."))
        assertEquals(TurnPolicy.Intent.SERIOUS, TurnPolicy.classify("I feel worried"))
        assertEquals(TurnPolicy.Intent.HOLD, TurnPolicy.classify("Attends une seconde"))
        assertEquals(TurnPolicy.Intent.HOLD, TurnPolicy.classify("wait"))
        assertTrue(TurnPolicy.quietChunks(TurnPolicy.Intent.THINKING) >
            TurnPolicy.quietChunks(TurnPolicy.Intent.NORMAL))
    }

    @Test fun gateRejectsNoiseAndSendsPrerollOnlyAfterStart() {
        val gate = AdaptiveActivityGate()
        assertTrue(gate.onAudio(quiet).packets.isEmpty())
        assertFalse(gate.onAudio(voiced).start)
        assertFalse(gate.onAudio(quiet).start)
        assertFalse(gate.onAudio(voiced).start)
        val start = gate.onAudio(voiced)
        assertTrue(start.start)
        assertEquals(3, start.packets.size)
        repeat(TurnPolicy.quietChunks(TurnPolicy.Intent.NORMAL) - 1) {
            assertFalse(gate.onAudio(quiet).end)
        }
        assertTrue(gate.onAudio(quiet).end)
        assertTrue(gate.onAudio(quiet).packets.isEmpty())
        assertFalse(gate.finish())
    }

    @Test fun explicitHoldAndResumedSpeechDoNotSplitTheTurn() {
        val gate = AdaptiveActivityGate()
        gate.onAudio(voiced)
        assertTrue(gate.onAudio(voiced).start)
        gate.onTranscript("hold on")
        repeat(50) { assertFalse(gate.onAudio(quiet).end) }
        assertFalse(gate.onAudio(voiced).end)
        repeat(TurnPolicy.quietChunks(TurnPolicy.Intent.NORMAL) - 1) {
            assertFalse(gate.onAudio(quiet).end)
        }
        assertTrue(gate.onAudio(quiet).end)
        assertFalse(gate.finish())
    }

    @Test fun muteAndResetCannotLeaveActivityOpen() {
        val gate = AdaptiveActivityGate()
        gate.onAudio(voiced)
        gate.onAudio(voiced)
        assertTrue(gate.finish())
        assertFalse(gate.finish())
        gate.reset()
        assertTrue(gate.onAudio(quiet).packets.isEmpty())
    }
}
