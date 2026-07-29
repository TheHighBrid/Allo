package com.kenza.callsim.call

/** Pure transition guards used to reject duplicate UI events before they can open resources. */
internal object CallTransitionRules {
    fun canPlaceCall(phase: CallPhase): Boolean = phase == CallPhase.IDLE

    fun canAnswerIncoming(phase: CallPhase): Boolean = phase == CallPhase.INCOMING
}
