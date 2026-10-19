package com.kenza.callsim.call

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KenzaVoiceStyleTest {

    private val tagPattern = Regex("\\[([a-zA-Z ]+)]")
    private val documentedV4Tags =
        setOf("laughs", "sighs", "exhales", "whispers", "curious", "sarcastic", "mischievously", "excited")

    @Test
    fun geminiStyleHasNoBracketTags() {
        assertTrue(tagPattern.findAll(KenzaVoiceStyle.forGemini()).none())
    }

    @Test
    fun plainElevenLabsStyleHasNoBracketTags() {
        assertTrue(tagPattern.findAll(KenzaVoiceStyle.forElevenLabs(expressiveTags = false)).none())
    }

    @Test
    fun expressiveStyleOnlyNamesDocumentedLowercaseTags() {
        val style = KenzaVoiceStyle.forElevenLabs(expressiveTags = true)
        val tags = tagPattern.findAll(style).map { it.groupValues[1] }.toList()
        assertTrue(tags.isNotEmpty())
        tags.forEach { assertTrue("undocumented tag $it", it in documentedV4Tags) }
    }

    @Test
    fun noEngineGetsContradictoryFillerRules() {
        listOf(KenzaVoiceStyle.forGemini(), KenzaVoiceStyle.forElevenLabs(true), KenzaVoiceStyle.forElevenLabs(false))
            .forEach { style ->
                assertFalse(style.contains("Never manufacture fillers"))
                assertTrue(style.contains("[[DIRECTOR"))
            }
    }
}
