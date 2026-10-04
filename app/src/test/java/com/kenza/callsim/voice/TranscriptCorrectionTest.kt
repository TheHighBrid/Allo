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
        assertEquals(listOf("user" to "hey", "agent" to CUT), assembler.commit())
    }

    @Test
    fun unknownOriginalLeavesTextUntouched() {
        val assembler = TranscriptAssembler()
        assembler.appendAgent(CUT)
        assertFalse(assembler.correctAgent("something else", "x"))
        assertFalse(assembler.correctAgent("  ", "x"))
        assertEquals(listOf("agent" to CUT), assembler.commit())
    }

    @Test
    fun correctsMostRecentCommittedAgentLine() {
        val entries = mutableListOf("agent" to FULL, "user" to "wait", "agent" to FULL)
        assertTrue(TranscriptAssembler.correctCommitted(entries, FULL, CUT))
        assertEquals(listOf("agent" to FULL, "user" to "wait", "agent" to CUT), entries)
    }

    @Test
    fun correctionToNothingRemovesTheLine() {
        val entries = mutableListOf("user" to "stop", "agent" to FULL)
        assertTrue(TranscriptAssembler.correctCommitted(entries, FULL, ""))
        assertEquals(listOf("user" to "stop"), entries)
        assertFalse(TranscriptAssembler.correctCommitted(entries, FULL, CUT))
    }

    private companion object {
        const val FULL = "So I was thinking we could go to the market tomorrow"
        const val CUT = "So I was thinking"
    }
}
