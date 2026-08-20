package com.kenza.callsim.script

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoScriptGeneratorTest {

    @Test
    fun `returns the same clearly-marked one-sided demo for the same request without a provider`() = runBlocking {
        val request = ScriptRequest(
            requestedMinutes = 10,
            mode = ScriptMode.PLAYFUL,
            language = "French",
            callReason = "A light catch-up after work",
        )
        val generator = DemoScriptGenerator()

        val first = generator.generate(request).getOrThrow()
        val second = generator.generate(request).getOrThrow()

        assertEquals(first, second)
        assertTrue(first.isDemo)
        assertEquals("French", first.language)
        assertEquals(ScriptMode.PLAYFUL, first.mode)
        assertTrue(first.ttsText.contains("[listening pause"))
        assertFalse(first.ttsText.contains("Listener:"))
        assertTrue(OneSidedDialogueValidator.validate(first.ttsText).isEmpty())
    }

    @Test
    fun `does not generate a demo when request validation fails`() = runBlocking {
        val result = DemoScriptGenerator().generate(ScriptRequest(requestedMinutes = 8))

        assertTrue(result.isFailure)
        assertEquals(
            "Choose a duration between 10 and 45 minutes.",
            result.exceptionOrNull()?.message,
        )
    }
}
