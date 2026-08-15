package com.kenza.callsim.schedule

/**
 * Coordinates every source of simulated calls through one shared call slot.
 * Scheduled arrivals are queued while an incoming, outgoing, connecting, or
 * active call occupies that slot.
 */
class ScheduledCallCoordinator(
    private val gate: ScheduledCallConcurrencyGate = ScheduledCallConcurrencyGate(),
) {
    fun tryStartUserInitiatedCall(): Boolean = gate.tryMarkCallOccupied()

    fun onScheduledCallDue(initiativeToken: String?): ScheduledCallArrival =
        gate.onScheduledCallDue(initiativeToken)

    fun onCallFinished(): DeferredScheduledCall? = gate.onCallBecameIdle()
}

/** Process-wide coordinator because alarms, the foreground ring service, and UI share one app process. */
object ScheduledCallRuntime {
    private val coordinator = ScheduledCallCoordinator()

    @Synchronized
    fun tryStartUserInitiatedCall(): Boolean = coordinator.tryStartUserInitiatedCall()

    @Synchronized
    fun onScheduledCallDue(initiativeToken: String?): ScheduledCallArrival =
        coordinator.onScheduledCallDue(initiativeToken)

    @Synchronized
    fun onCallFinished(): DeferredScheduledCall? = coordinator.onCallFinished()
}
