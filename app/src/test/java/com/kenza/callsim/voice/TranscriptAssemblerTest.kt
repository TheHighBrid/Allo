package com.kenza.callsim.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TranscriptAssemblerTest {
    @Test fun mergesOverlapAndCommitsOnce() {
        val assembler = TranscriptAssembler()
        assembler.appendUser("hello wor")
        assembler.appendUser("world")
        assembler.appendAgent("hi")
        assembler.appendAgent("hi")
        assertEquals(listOf("user" to "hello world", "agent" to "hi"), assembler.commit())
        assertTrue(assembler.commit().isEmpty())
    }

    @Test fun nextTurnDoesNotRepeatPreviouslyCommittedText() {
        val assembler = TranscriptAssembler()
        assembler.appendUser("first turn")
        assertEquals(listOf("user" to "first turn"), assembler.commit())
        assembler.appendUser("second turn")
        assertEquals(listOf("user" to "second turn"), assembler.commit())
    }

    @Test fun clearDropsUncommittedFragments() {
        val assembler = TranscriptAssembler()
        assembler.appendAgent("not delivered")
        assembler.clear()
        assertTrue(assembler.commit().isEmpty())
    }
}
