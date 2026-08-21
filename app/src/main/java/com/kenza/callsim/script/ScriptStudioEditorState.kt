package com.kenza.callsim.script

/**
 * Provider-independent state for Script Studio's editable request and output.
 * The Compose screen owns presentation changes, while this type centralizes the
 * derived duration and safety information used by the editor.
 */
data class ScriptStudioEditorState(
    val requestedMinutes: Int = 10,
    val mode: ScriptMode = ScriptMode.CASUAL_DAILY,
    val language: String = "English",
    val callReason: String = "",
    val scriptText: String = "",
) {
    val duration: ScriptDurationEstimate
        get() = ScriptDurationEstimator.estimate(scriptText)

    val warnings: List<OneSidedDialogueWarning>
        get() = OneSidedDialogueValidator.validate(scriptText)

    fun toRequest(): ScriptRequest = ScriptRequest(
        requestedMinutes = requestedMinutes,
        mode = mode,
        language = language,
        callReason = callReason.ifBlank { null },
    )
}
