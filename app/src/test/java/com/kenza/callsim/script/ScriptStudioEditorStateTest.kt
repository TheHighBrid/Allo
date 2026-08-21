package com.kenza.callsim.script

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScriptStudioEditorStateTest {

    @Test
    fun `edited script text recalculates duration and exposes one-sided dialogue warnings`() {
        val editor = ScriptStudioEditorState(
            scriptText = "Kenza: Okay, I hear you. [pause 4 seconds] Tell me what happened.",
        )

        assertEquals(3, editor.duration.speechSeconds)
        assertEquals(4, editor.duration.listenerPauseSeconds)
        assertEquals(7, editor.duration.totalSeconds)
        assertEquals(OneSidedDialogueWarningCode.SPEAKER_LABEL, editor.warnings.single().code)
    }

    @Test
    fun `builds a script request from the current editable controls`() {
        val editor = ScriptStudioEditorState(
            requestedMinutes = 20,
            mode = ScriptMode.CUSTOM,
            language = "French",
            callReason = "An affectionate weekend catch-up",
        )

        val request = editor.toRequest()

        assertEquals(20, request.requestedMinutes)
        assertEquals(ScriptMode.CUSTOM, request.mode)
        assertEquals("French", request.language)
        assertEquals("An affectionate weekend catch-up", request.callReason)
        assertTrue(ScriptRequestValidator.validate(request).isValid)
    }
}
