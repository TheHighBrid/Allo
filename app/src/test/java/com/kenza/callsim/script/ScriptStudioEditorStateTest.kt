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
    fun `builds a structured script request from current editable controls`() {
        val editor = ScriptStudioEditorState(
            requestedMinutes = 20,
            mode = ScriptMode.CUSTOM,
            language = "French",
            callReason = "An affectionate weekend catch-up",
            mood = "playful and tired",
            topicsText = "Melato launch, weekend plans\nfamily",
            selectedMemoryIds = listOf("memory-a", "memory-b", "memory-a"),
            affection = IntensityLevel.HIGH,
            humor = IntensityLevel.MODERATE,
            flirtation = IntensityLevel.LOW,
            boundariesText = "Do not invent plans\nNo pressure",
            endingStyle = "soft goodnight",
        )

        val request = editor.toRequest()

        assertEquals(20, request.requestedMinutes)
        assertEquals(ScriptMode.CUSTOM, request.mode)
        assertEquals("French", request.language)
        assertEquals("An affectionate weekend catch-up", request.callReason)
        assertEquals("playful and tired", request.kenzaMood)
        assertEquals(listOf("Melato launch", "weekend plans", "family"), request.mainTopics)
        assertEquals(listOf("memory-a", "memory-b"), request.selectedMemoryIds)
        assertEquals(IntensityLevel.HIGH, request.affection)
        assertEquals(IntensityLevel.MODERATE, request.humor)
        assertEquals(IntensityLevel.LOW, request.flirtation)
        assertEquals(listOf("Do not invent plans", "No pressure"), request.boundaries)
        assertEquals("soft goodnight", request.endingStyle)
        assertTrue(ScriptRequestValidator.validate(request).isValid)
    }
}
