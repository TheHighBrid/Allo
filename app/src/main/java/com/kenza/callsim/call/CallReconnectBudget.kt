package com.kenza.callsim.call

/**
 * Caps only *consecutive* fresh-session recovery failures. A confirmed provider-ready
 * event resets this budget, so intermittent network drops over a long call do not
 * silently accumulate until a healthy call is terminated.
 */
internal class CallReconnectBudget(private val maxAttempts: Int = 4) {
    init { require(maxAttempts > 0) }

    var attempts: Int = 0
        private set

    fun nextDelayMs(): Long? {
        if (attempts >= maxAttempts) return null
        return 250L * (1L shl (attempts++).coerceAtMost(3))
    }

    fun reset() {
        attempts = 0
    }
}
