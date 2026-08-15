package com.kenza.callsim.call

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationEndDetectorTest {
    @Test
    fun `recognizes explicit farewells despite punctuation and curly apostrophes`() {
        assertTrue(ConversationEndDetector.isFarewell("  Good night!! "))
        assertTrue(ConversationEndDetector.isFarewell("I’ll call you later."))
        assertTrue(ConversationEndDetector.isFarewell("Okay, bye babe."))
    }

    @Test
    fun `does not hang up for phrases used in ordinary conversation`() {
        assertFalse(ConversationEndDetector.isFarewell("I can't see you in that photo"))
        assertFalse(ConversationEndDetector.isFarewell("I'll call you later in the afternoon to confirm"))
        assertFalse(ConversationEndDetector.isAngryHangup("I'm done with work"))
        assertFalse(ConversationEndDetector.isAngryHangup("Don't ever give up on that idea"))
    }

    @Test
    fun `recognizes explicit angry hangups only as complete utterances`() {
        assertTrue(ConversationEndDetector.isAngryHangup("Leave me alone!"))
        assertTrue(ConversationEndDetector.isAngryHangup("I'm hanging up."))
    }
}
