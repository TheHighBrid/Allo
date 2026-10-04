package com.kenza.callsim.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ElevenLabsClientMessagesTest {

    @Test
    fun initiationCarriesDynamicVariablesWithoutOverrideByDefault() {
        val init = ElevenLabsClientMessages.initiation(mapOf("opening_line" to "Allô?"), promptOverride = null)
        assertEquals("conversation_initiation_client_data", init.getString(TYPE))
        assertEquals("Allô?", init.getJSONObject("dynamic_variables").getString("opening_line"))
        assertFalse(init.has(OVERRIDE))
    }

    @Test
    fun initiationIncludesPromptOverrideOnlyWhenProvided() {
        val init = ElevenLabsClientMessages.initiation(emptyMap(), promptOverride = "persona")
        assertFalse(init.has("dynamic_variables"))
        assertEquals(
            "persona",
            init.getJSONObject(OVERRIDE).getJSONObject("agent").getJSONObject("prompt").getString("prompt"),
        )
        assertFalse(ElevenLabsClientMessages.initiation(emptyMap(), promptOverride = " ").has(OVERRIDE))
    }

    @Test
    fun contextualUpdateIsNotAUserMessage() {
        val update = ElevenLabsClientMessages.contextualUpdate("[[DIRECTOR: check in]]")
        assertEquals("contextual_update", update.getString(TYPE))
        assertEquals("[[DIRECTOR: check in]]", update.getString("text"))
        assertFalse(update.has("context_id"))
        assertEquals("ctx", ElevenLabsClientMessages.contextualUpdate("x", "ctx").getString("context_id"))
        assertEquals("user_message", ElevenLabsClientMessages.userMessage("hi").getString(TYPE))
    }

    @Test
    fun pongEchoesEventIdAndSampleRateParses() {
        val pong = ElevenLabsClientMessages.pong(42)
        assertEquals("pong", pong.getString(TYPE))
        assertEquals(42, pong.getInt("event_id"))
        assertEquals(16_000, ElevenLabsClientMessages.sampleRateOf(ElevenLabsClientMessages.EXPECTED_INPUT_FORMAT))
        assertEquals(24_000, ElevenLabsClientMessages.sampleRateOf("pcm_24000"))
        assertEquals(8_000, ElevenLabsClientMessages.sampleRateOf("ulaw_8000"))
        assertEquals(16_000, ElevenLabsClientMessages.sampleRateOf("mystery"))
    }

    private companion object {
        const val TYPE = "type"
        const val OVERRIDE = "conversation_config_override"
    }
}
