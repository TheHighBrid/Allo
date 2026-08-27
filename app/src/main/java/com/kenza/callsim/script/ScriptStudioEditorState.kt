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
    val mood: String = "",
    val topicsText: String = "",
    val selectedMemoryIds: List<String> = emptyList(),
    val affection: IntensityLevel = IntensityLevel.MODERATE,
    val humor: IntensityLevel = IntensityLevel.MODERATE,
    val flirtation: IntensityLevel = IntensityLevel.LOW,
    val boundariesText: String = "",
    val endingStyle: String = "",
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
        relationshipMood = mood.ifBlank { null },
        mainTopics = topicsText.toListField(),
        selectedMemoryIds = selectedMemoryIds.distinct(),
        kenzaMood = mood.ifBlank { null },
        affection = affection,
        humor = humor,
        flirtation = flirtation,
        boundaries = boundariesText.toListField(),
        endingStyle = endingStyle.ifBlank { null },
    )
}

internal fun String.toListField(): List<String> = split('\n', ',', ';')
    .map { it.trim() }
    .filter { it.isNotBlank() }
    .distinct()
