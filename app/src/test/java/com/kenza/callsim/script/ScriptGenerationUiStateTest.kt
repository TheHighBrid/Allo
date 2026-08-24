package com.kenza.callsim.script

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScriptGenerationUiStateTest {

    @Test
    fun `Gemini generation shows Gemini progress and leaves the demo label unchanged`() {
        val state = ScriptGenerationUiState.idle().start(ScriptGenerationSource.GEMINI)

        assertTrue(state.isGenerating)
        assertEquals("Creating demo script", state.demoButtonLabel)
        assertEquals("Generating with Gemini…", state.geminiButtonLabel)
    }

    @Test
    fun `finishing a generation always restores an interactive idle state`() {
        val state = ScriptGenerationUiState.idle()
            .start(ScriptGenerationSource.GEMINI)
            .finish()

        assertFalse(state.isGenerating)
        assertEquals("Generate with Gemini", state.geminiButtonLabel)
    }
}
