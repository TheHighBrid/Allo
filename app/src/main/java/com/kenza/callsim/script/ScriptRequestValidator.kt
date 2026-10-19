package com.kenza.callsim.script

/** The result of checking whether a request is safe and ready for generation. */
data class ScriptRequestValidationResult(
    val errors: List<String>,
) {
    val isValid: Boolean get() = errors.isEmpty()
}

/**
 * Performs local validation before a generation provider is selected or called.
 * This prevents avoidable costs and gives callers actionable feedback.
 */
object ScriptRequestValidator {

    fun validate(request: ScriptRequest): ScriptRequestValidationResult {
        val errors = buildList {
            if (request.requestedMinutes !in MIN_DURATION_MINUTES..MAX_DURATION_MINUTES) {
                add("Choose a duration between 10 and 45 minutes.")
            }
            if (request.language.isBlank()) {
                add("Choose a language for the script.")
            }
            if (request.mode == ScriptMode.CUSTOM && request.callReason.isNullOrBlank()) {
                add("Describe the scenario or reason for this custom call.")
            }
        }
        return ScriptRequestValidationResult(errors)
    }

    private const val MIN_DURATION_MINUTES = 10
    private const val MAX_DURATION_MINUTES = 45
}
