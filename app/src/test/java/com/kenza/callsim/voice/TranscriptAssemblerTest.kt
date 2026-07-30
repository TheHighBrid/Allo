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

    @Test fun clearDropsUncommittedFragments() {
        val assembler = TranscriptAssembler()
        assembler.appendAgent("not delivered")
        assembler.clear()
        assertTrue(assembler.commit().isEmpty())
    }
}
