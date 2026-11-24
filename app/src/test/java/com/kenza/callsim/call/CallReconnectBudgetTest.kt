package com.kenza.callsim.call

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CallReconnectBudgetTest {
    @Test fun repeatedFailuresAreBounded() {
        val budget = CallReconnectBudget()
        assertEquals(250L, budget.nextDelayMs())
        assertEquals(500L, budget.nextDelayMs())
        assertEquals(1000L, budget.nextDelayMs())
        assertEquals(2000L, budget.nextDelayMs())
        assertNull(budget.nextDelayMs())
        assertEquals(4, budget.attempts)
    }

    @Test fun successfulReconnectRestoresBudget() {
        val budget = CallReconnectBudget()
        budget.nextDelayMs()
        budget.nextDelayMs()
        budget.reset()
        assertEquals(0, budget.attempts)
        assertEquals(250L, budget.nextDelayMs())
    }

    @Test fun freshCallAlsoRestoresBudget() {
        val budget = CallReconnectBudget(maxAttempts = 1)
        assertEquals(250L, budget.nextDelayMs())
        assertNull(budget.nextDelayMs())
        budget.reset()
        assertEquals(250L, budget.nextDelayMs())
    }
}
