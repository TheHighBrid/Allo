package com.kenza.callsim.call

import org.junit.Test
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

class ConversationWatchersTest {
    @Test
    fun outgoingCallCanOnlyStartWhileIdle() {
        assertTrue(CallTransitionRules.canPlaceCall(CallPhase.IDLE))
        CallPhase.entries.filterNot { it == CallPhase.IDLE }.forEach { phase ->
            assertFalse(
                "unexpected outgoing transition from $phase",
                CallTransitionRules.canPlaceCall(phase),
            )
        }
    }

    @Test
    fun incomingCallCanOnlyBeResolvedWhileRinging() {
        assertTrue(CallTransitionRules.canAnswerIncoming(CallPhase.INCOMING))
        CallPhase.entries.filterNot { it == CallPhase.INCOMING }.forEach { phase ->
            assertFalse(
                "unexpected incoming transition from $phase",
                CallTransitionRules.canAnswerIncoming(phase),
            )
        }
    }
}
