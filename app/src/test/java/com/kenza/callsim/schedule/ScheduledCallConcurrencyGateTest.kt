package com.kenza.callsim.schedule

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScheduledCallConcurrencyGateTest {

    @Test
    fun scheduledCallDueDuringActiveCallIsDeferredUntilTheCallBecomesIdle() {
        val gate = ScheduledCallConcurrencyGate()
        gate.markCallOccupied()

        assertEquals(
            ScheduledCallArrival.DEFERRED,
            gate.onScheduledCallDue(initiativeToken = "after-current-call"),
        )

        assertEquals(
            DeferredScheduledCall("after-current-call"),
            gate.onCallBecameIdle(),
        )
    }

    @Test
    fun onlyOneScheduledCallCanRingAtATimeAndDeferredCallsRemainInOrder() {
        val gate = ScheduledCallConcurrencyGate()

        assertEquals(ScheduledCallArrival.RING_NOW, gate.onScheduledCallDue(null))
        assertEquals(ScheduledCallArrival.DEFERRED, gate.onScheduledCallDue("second"))
        assertEquals(ScheduledCallArrival.DEFERRED, gate.onScheduledCallDue("third"))

        assertEquals(DeferredScheduledCall("second"), gate.onCallBecameIdle())
        assertEquals(DeferredScheduledCall("third"), gate.onCallBecameIdle())
        assertNull(gate.onCallBecameIdle())
    }
}

class ScheduledCallCoordinatorTest {

    @Test
    fun activeOutgoingCallDefersTheScheduleAndReleasesItWhenTheCallEnds() {
        val coordinator = ScheduledCallCoordinator()

        assertEquals(true, coordinator.tryStartUserInitiatedCall())
        assertEquals(
            ScheduledCallArrival.DEFERRED,
            coordinator.onScheduledCallDue(initiativeToken = "scheduled-after-outgoing"),
        )
        assertEquals(
            DeferredScheduledCall("scheduled-after-outgoing"),
            coordinator.onCallFinished(),
        )
    }
}
