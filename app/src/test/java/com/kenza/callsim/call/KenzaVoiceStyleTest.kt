package com.kenza.callsim.call

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KenzaVoiceStyleTest {

    @Test
    fun `gemini style keeps spoken rules and stays tag-free`() {
        val style = KenzaVoiceStyle.forGemini()
        assertTrue(style.contains("DELIVERY:"))
        assertTrue(style.contains("QUESTION BUDGET:"))
        assertTrue(style.contains("VARIETY:"))
        assertFalse(style.contains("[laughs]"))
        assertFalse(style.contains("[sighs]"))
    }

    @Test
    fun `expressive elevenlabs style allows documented tags only`() {
        val style = KenzaVoiceStyle.forElevenLabs(expressiveTags = true)
        assertTrue(style.contains("[laughs]"))
        assertFalse(style.contains("[screams]"))
    }

    @Test
    fun `plain elevenlabs style forbids tags`() {
        val style = KenzaVoiceStyle.forElevenLabs(expressiveTags = false)
        assertFalse(style.contains("[laughs]"))
        assertFalse(style.contains("[sighs]"))
    }

    @Test
    fun `all styles carry the assistant-voice prohibition`() {
        val styles = listOf(
            KenzaVoiceStyle.forGemini(),
            KenzaVoiceStyle.forElevenLabs(expressiveTags = true),
            KenzaVoiceStyle.forElevenLabs(expressiveTags = false),
        )
        for (style in styles) {
            assertTrue(style.contains("Never sound like an assistant"))
            assertTrue(style.contains("VARIETY:"))
        }
    }
}
