package com.kenza.callsim.call

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationRepairEngineTest {

    @Test
    fun `silence never emits a proactive presence check`() {
        val engine = ConversationRepairEngine()
        engine.onAgentText("What time should we meet?")
        engine.onAgentTurnComplete(nowMs = 0)

        assertNull(engine.onSilenceElapsed(nowMs = 6_499))
        assertNull(engine.onSilenceElapsed(nowMs = 6_500))
        assertNull(engine.onSilenceElapsed(nowMs = 20_000))
    }

    @Test
    fun `silence keeps the pending question available for real user input`() {
        val engine = ConversationRepairEngine()
        engine.onAgentText("What time should we meet?")
        engine.onAgentTurnComplete(nowMs = 0)

        assertNull(engine.onSilenceElapsed(nowMs = 6_500))
        assertNull(engine.onSilenceElapsed(nowMs = 20_000))

        val action = engine.onUserText("By the way, did you see the new movie?")

        assertEquals(ConversationRepairAction.Kind.UNANSWERED_QUESTION, action?.kind)
        assertTrue(action?.directorCue?.contains("What time should we meet?") == true)
    }

    @Test
    fun `gently returns to the question when presence is confirmed without an answer`() {
        val engine = ConversationRepairEngine()
        engine.onAgentText("What time should we meet?")
        engine.onAgentTurnComplete(nowMs = 0)
        engine.onSilenceElapsed(nowMs = 6_500)

        val action = engine.onUserText("Yeah, I'm still here.")

        assertEquals(ConversationRepairAction.Kind.UNANSWERED_QUESTION, action?.kind)
        assertTrue(action?.directorCue?.contains("What time should we meet?") == true)
    }

    @Test
    fun `asks the model to recover an unanswered question when the user changes subject`() {
        val engine = ConversationRepairEngine()
        engine.onAgentText("What time should we meet?")
        engine.onAgentTurnComplete(nowMs = 0)

        val action = engine.onUserText("By the way, did you see the new movie?")

        assertEquals(ConversationRepairAction.Kind.UNANSWERED_QUESTION, action?.kind)
        assertTrue(action?.directorCue?.contains("What time should we meet?") == true)
    }

    @Test
    fun `gently returns to a time question after an unrelated statement`() {
        val engine = ConversationRepairEngine()
        engine.onAgentText("What time should we meet?")
        engine.onAgentTurnComplete(nowMs = 0)

        val action = engine.onUserText("I really liked that new movie.")

        assertEquals(ConversationRepairAction.Kind.UNANSWERED_QUESTION, action?.kind)
        assertTrue(action?.directorCue?.contains("What time should we meet?") == true)
    }

    @Test
    fun `acknowledges an abrupt farewell when a question is still unresolved`() {
        val engine = ConversationRepairEngine()
        engine.onAgentText("What time should we meet?")
        engine.onAgentTurnComplete(nowMs = 0)

        val action = engine.onUserText("Bye")

        assertEquals(ConversationRepairAction.Kind.ABRUPT_FAREWELL, action?.kind)
        assertTrue(action?.directorCue?.contains("abruptly") == true)
    }

    @Test
    fun `treats an angry hangup as an abrupt farewell when a question is pending`() {
        val engine = ConversationRepairEngine()
        engine.onAgentText("What time should we meet?")
        engine.onAgentTurnComplete(nowMs = 0)

        val action = engine.onUserText("I'm hanging up")

        assertEquals(ConversationRepairAction.Kind.ABRUPT_FAREWELL, action?.kind)
        assertTrue(action?.directorCue?.contains("composed goodbye") == true)
    }

    @Test
    fun `softens recovery when the user likely answered the pending question`() {
        val engine = ConversationRepairEngine()
        engine.onAgentText("What time should we meet?")
        engine.onAgentTurnComplete(nowMs = 0)

        val action = engine.onUserText("Around 7 pm")

        assertEquals(ConversationRepairAction.Kind.UNANSWERED_QUESTION, action?.kind)
        assertTrue(action?.directorCue?.contains("may already answer") == true)
    }
}
