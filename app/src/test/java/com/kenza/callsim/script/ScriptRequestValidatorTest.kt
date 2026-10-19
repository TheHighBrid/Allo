package com.kenza.callsim.script

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScriptRequestValidatorTest {

    @Test
    fun `rejects requests outside the supported ten to forty-five minute range`() {
        val tooShort = ScriptRequest(requestedMinutes = 9)
        val tooLong = ScriptRequest(requestedMinutes = 46)

        assertEquals(
            listOf("Choose a duration between 10 and 45 minutes."),
            ScriptRequestValidator.validate(tooShort).errors,
        )
        assertEquals(
            listOf("Choose a duration between 10 and 45 minutes."),
            ScriptRequestValidator.validate(tooLong).errors,
        )
    }

    @Test
    fun `requires a call reason when custom mode is selected`() {
        val request = ScriptRequest(
            requestedMinutes = 10,
            mode = ScriptMode.CUSTOM,
            callReason = "   ",
        )

        assertEquals(
            listOf("Describe the scenario or reason for this custom call."),
            ScriptRequestValidator.validate(request).errors,
        )
    }

    @Test
    fun `rejects requests with a blank language`() {
        val request = ScriptRequest(
            requestedMinutes = 10,
            language = " ",
        )

        val result = ScriptRequestValidator.validate(request)

        assertTrue(result.errors.contains("Choose a language for the script."))
    }

    @Test
    fun `accepts a complete default request`() {
        val result = ScriptRequestValidator.validate(
            ScriptRequest(requestedMinutes = 10, language = "English"),
        )

        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }
}
