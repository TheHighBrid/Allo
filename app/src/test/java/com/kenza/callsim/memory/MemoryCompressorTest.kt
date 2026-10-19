package com.kenza.callsim.memory

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MemoryCompressorTest {

    private fun item(id: String, text: String, importance: Int = 2) = MemoryItem(
        id = id,
        kind = MemoryKind.FACT,
        text = text,
        createdAt = 1L,
        importance = importance,
    )

    @Test
    fun defaultCompressorIsInertAndNeverInventsText() = runBlocking {
        val compressor = MemoryCompressor()
        assertFalse(compressor.isEnabled)
        assertNull(compressor.compress(listOf(item("a", "Old fact"))))
    }

    @Test
    fun summarizerOutputBecomesSharedNarrative() = runBlocking {
        var seenPrompt = ""
        val compressor = MemoryCompressor(
            summarize = { prompt -> seenPrompt = prompt; "  They met in Ottawa\nand  kept talking. " },
            clock = { 42L },
        )
        val narrative = compressor.compress(listOf(item("a", "Met in Ottawa", 3)))!!
        assertTrue(compressor.isEnabled)
        assertEquals(MemoryKind.NARRATIVE, narrative.kind)
        assertEquals(MemoryOwner.SHARED, narrative.owner)
        assertEquals("They met in Ottawa and kept talking.", narrative.text)
        assertEquals(42L, narrative.createdAt)
        assertTrue(seenPrompt.contains("- Met in Ottawa (Importance: 3)"))
    }

    @Test
    fun blankOrMissingSummaryIsDiscarded() = runBlocking {
        assertNull(MemoryCompressor(summarize = { "   " }).compress(listOf(item("a", "x"))))
        assertNull(MemoryCompressor(summarize = { null }).compress(listOf(item("a", "x"))))
        assertNull(MemoryCompressor(summarize = { "unused" }).compress(emptyList()))
    }

    @Test
    fun narrativeIsCappedToStoreLimit() = runBlocking {
        val narrative = MemoryCompressor(summarize = { "x".repeat(2_000) }).compress(listOf(item("a", "x")))!!
        assertEquals(MemoryCompressor.MAX_NARRATIVE_CHARS, narrative.text.length)
    }
}
