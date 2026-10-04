package com.kenza.callsim.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TranscriptCorrectionTest {

    @Test
    fun correctsUncommittedAgentText() {
        val assembler = TranscriptAssembler()
        assembler.appendUser("hey")
        assembler.appendAgent(FULL)
        assertTrue(assembler.correctAgent(FULL, CUT))
        assertEquals(listOf(USER to "hey", AGENT to CUT), assembler.commit())
    }

    @Test
    fun unknownOriginalLeavesTextUntouched() {
        val assembler = TranscriptAssembler()
        assembler.appendAgent(CUT)
        assertFalse(assembler.correctAgent("something else", "x"))
        assertFalse(assembler.correctAgent("  ", "x"))
        assertEquals(listOf(AGENT to CUT), assembler.commit())
    }

    @Test
    fun correctsMostRecentCommittedAgentLine() {
        val entries = mutableListOf(AGENT to FULL, USER to "wait", AGENT to FULL)
        assertTrue(TranscriptAssembler.correctCommitted(entries, FULL, CUT))
        assertEquals(listOf(AGENT to FULL, USER to "wait", AGENT to CUT), entries)
    }

    @Test
    fun correctionToNothingRemovesTheLine() {
        val entries = mutableListOf(USER to "stop", AGENT to FULL)
        assertTrue(TranscriptAssembler.correctCommitted(entries, FULL, ""))
        assertEquals(listOf(USER to "stop"), entries)
        assertFalse(TranscriptAssembler.correctCommitted(entries, FULL, CUT))
    }

    private companion object {
        const val AGENT = "agent"
        const val USER = "user"
        const val FULL = "So I was thinking we could go to the market tomorrow"
        const val CUT = "So I was thinking"
    }
}
