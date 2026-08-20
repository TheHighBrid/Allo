package com.kenza.callsim.script

/** The high-level conversational shape requested for a generated script. */
enum class ScriptMode {
    CASUAL_DAILY,
    BEFORE_SLEEP,
    MISSING_YOU,
    PLAYFUL,
    SUPPORTIVE,
    RELATIONSHIP_CHECK_IN,
    STORYTELLING,
    FUTURE_PLANNING,
    AFTER_ARGUMENT,
    SURPRISE,
    CUSTOM,
}

/**
 * A provider-independent script request.
 *
 * The model intentionally contains only fields needed to validate an initial
 * request. Additional creative controls can be added without coupling callers
 * to a particular generation provider.
 */
data class ScriptRequest(
    val requestedMinutes: Int,
    val mode: ScriptMode = ScriptMode.CASUAL_DAILY,
    val language: String = "English",
    val callReason: String? = null,
)
