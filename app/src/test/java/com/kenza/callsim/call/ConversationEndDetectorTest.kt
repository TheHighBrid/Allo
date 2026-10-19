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
        assertTrue(ConversationEndDetector.isFarewell("Gotta run"))
        assertTrue(ConversationEndDetector.isFarewell("Catch you later."))
        assertTrue(ConversationEndDetector.isFarewell("I'm heading out"))
    }

    @Test
    fun `does not hang up for phrases used in ordinary conversation`() {
        assertFalse(ConversationEndDetector.isFarewell("I can't see you in that photo"))
        assertFalse(ConversationEndDetector.isFarewell("I'll call you later in the afternoon to confirm"))
        assertFalse(ConversationEndDetector.isFarewell("I have to go to the store later"))
        assertFalse(ConversationEndDetector.isAngryHangup("I'm done with work"))
        assertFalse(ConversationEndDetector.isAngryHangup("Don't ever give up on that idea"))
        assertFalse(ConversationEndDetector.isAngryHangup("We're through the worst of it"))
    }

    @Test
    fun `recognizes explicit angry hangups only as complete utterances`() {
        assertTrue(ConversationEndDetector.isAngryHangup("Leave me alone!"))
        assertTrue(ConversationEndDetector.isAngryHangup("I'm hanging up."))
        assertTrue(ConversationEndDetector.isAngryHangup("Hanging up now"))
        assertTrue(ConversationEndDetector.isAngryHangup("This call is over"))
    }
}
