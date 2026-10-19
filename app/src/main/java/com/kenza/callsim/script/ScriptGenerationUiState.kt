package com.kenza.callsim.script

enum class ScriptGenerationSource {
    DEMO,
    GEMINI,
}

/** Pure UI state that prevents Gemini work from being presented as demo generation. */
data class ScriptGenerationUiState(
    val activeSource: ScriptGenerationSource? = null,
) {
    val isGenerating: Boolean
        get() = activeSource != null

    val demoButtonLabel: String
        get() = if (activeSource == ScriptGenerationSource.DEMO) "Creating demo…" else "Creating demo script"

    val geminiButtonLabel: String
        get() = if (activeSource == ScriptGenerationSource.GEMINI) "Generating with Gemini…" else "Generate with Gemini"

    fun start(source: ScriptGenerationSource): ScriptGenerationUiState = copy(activeSource = source)

    fun finish(): ScriptGenerationUiState = copy(activeSource = null)

    companion object {
        fun idle(): ScriptGenerationUiState = ScriptGenerationUiState()
    }
}
