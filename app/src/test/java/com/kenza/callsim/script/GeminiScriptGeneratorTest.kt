package com.kenza.callsim.script

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiScriptGeneratorTest {

    @Test
    fun `generates a non-demo script through the configured Gemini model`() = runBlocking {
        val transport = RecordingTransport(Result.success("Hey, I was hoping I would catch you. [listening pause 3 seconds]"))
        val generator = GeminiScriptGenerator(
            apiKey = "test-key",
            model = "gemini-test-model",
            transport = transport,
        )

        val result = generator.generate(
            ScriptRequest(
                requestedMinutes = 10,
                mode = ScriptMode.PLAYFUL,
                language = "French",
                callReason = "A light evening catch-up",
            ),
        ).getOrThrow()

        assertFalse(result.isDemo)
        assertEquals("French", result.language)
        assertEquals(ScriptMode.PLAYFUL, result.mode)
        assertEquals("gemini-test-model", transport.model)
        assertTrue(transport.prompt.contains("Only output Kenza's audible side."))
        assertTrue(transport.prompt.contains("A light evening catch-up"))
    }

    @Test
    fun `rejects invalid requests before the Gemini transport is called`() = runBlocking {
        val transport = RecordingTransport(Result.success("This must not be used."))
        val generator = GeminiScriptGenerator(apiKey = "test-key", transport = transport)

        val result = generator.generate(ScriptRequest(requestedMinutes = 8))

        assertTrue(result.isFailure)
        assertEquals(0, transport.calls)
    }

    private class RecordingTransport(
        private val response: Result<String>,
    ) : GeminiScriptTransport {
        var calls = 0
        var model = ""
        var prompt = ""

        override suspend fun generate(model: String, apiKey: String, prompt: String): Result<String> {
            calls += 1
            this.model = model
            this.prompt = prompt
            return response
        }
    }
}
