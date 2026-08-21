package com.kenza.callsim.call

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationRepairEngineTest {

    @Test
    fun `issues one presence check after a question receives no response for six and a half seconds`() {
        val engine = ConversationRepairEngine()
        engine.onAgentText("What time should we meet?")
        engine.onAgentTurnComplete(nowMs = 0)

        assertNull(engine.onSilenceElapsed(nowMs = 6_499))
        val action = engine.onSilenceElapsed(nowMs = 6_500)

        assertEquals(ConversationRepairAction.Kind.PRESENCE_CHECK, action?.kind)
        assertTrue(action?.directorCue?.contains("Are you still there") == true)
        assertNull(engine.onSilenceElapsed(nowMs = 20_000))
    }

    @Test
    fun `does not track the agent turn created by a repair cue as a new unanswered question`() {
        val engine = ConversationRepairEngine()
        engine.onAgentText("What time should we meet?")
        engine.onAgentTurnComplete(nowMs = 0)

        val action = engine.onSilenceElapsed(nowMs = 6_500)
        engine.onRepairActionDispatched(action!!)
        engine.onAgentText("Are you still there?")
        engine.onAgentTurnComplete(nowMs = 6_600)

        assertNull(engine.onSilenceElapsed(nowMs = 13_100))
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
}
