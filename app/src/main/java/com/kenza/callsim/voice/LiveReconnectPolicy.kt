package com.kenza.callsim.voice

/** Public Live protocol, without speculative replay or undocumented setup fields. */
internal object LiveReconnectPolicy {
    const val MAX_ATTEMPTS = 4
    const val POLL_MS = 100L

    fun canRotate(hasHandle: Boolean, checkpointCurrent: Boolean, userSpeaking: Boolean,
                  modelGenerating: Boolean, deadlineReached: Boolean): Boolean =
        hasHandle && (deadlineReached || (checkpointCurrent && !userSpeaking && !modelGenerating))

    fun retryDelayMs(attempt: Int, planned: Boolean): Long =
        if (planned) 0L else 250L * (1L shl (attempt - 1).coerceIn(0, 3))

    fun goAwayDelayMs(value: Any?): Long {
        val seconds = when (value) {
            is Number -> value.toDouble()
            is String -> value.removeSuffix("s").toDoubleOrNull()
            else -> null
        } ?: 4.0
        return (seconds * 1000 - 500).toLong().coerceIn(0L, 60_000L)
    }
}
