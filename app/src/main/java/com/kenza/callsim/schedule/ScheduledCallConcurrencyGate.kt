package com.kenza.callsim.schedule

/**
 * Serializes all call attempts into one app-wide call slot. A scheduled call
 * reserves the slot while it is ringing, so later schedules cannot start their
 * own ringtone or vibration until the current call has ended.
 */
class ScheduledCallConcurrencyGate {
    private var callOccupied = false
    private val deferredCalls = ArrayDeque<DeferredScheduledCall>()

    /** Marks an already-established outgoing or simulated call as occupying the slot. */
    fun markCallOccupied() {
        callOccupied = true
    }

    /** Tries to reserve the call slot before a new outgoing or simulated incoming call begins. */
    fun tryMarkCallOccupied(): Boolean {
        if (callOccupied) return false
        callOccupied = true
        return true
    }

    /**
     * Reserves the call slot for an arriving schedule, or queues it behind the
     * current ringing/active call when the slot is already occupied.
     */
    fun onScheduledCallDue(initiativeToken: String?): ScheduledCallArrival {
        if (!callOccupied) {
            callOccupied = true
            return ScheduledCallArrival.RING_NOW
        }
        deferredCalls.addLast(DeferredScheduledCall(initiativeToken))
        return ScheduledCallArrival.DEFERRED
    }

    /**
     * Releases the completed call and returns the next deferred schedule, if
     * any. Returning it reserves the slot again before it starts ringing.
     */
    fun onCallBecameIdle(): DeferredScheduledCall? {
        val next = deferredCalls.removeFirstOrNull()
        callOccupied = next != null
        return next
    }
}

enum class ScheduledCallArrival { RING_NOW, DEFERRED }

data class DeferredScheduledCall(val initiativeToken: String?)
