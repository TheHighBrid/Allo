package com.kenza.callsim.memory

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MemoryPolicyTest {

    @Test
    fun nearDuplicate_ignoresCaseAndPunctuation() {
        assertTrue(
            MemoryPolicy.isNearDuplicate(
                "Mohamed prefers relaxed flare jeans.",
                "mohamed prefers relaxed flare jeans",
            )
        )
    }

    @Test
    fun nearDuplicate_keepsDifferentFactsSeparate() {
        assertFalse(
            MemoryPolicy.isNearDuplicate(
                "Mohamed has an interview on Thursday",
                "Mohamed wants to visit Montreal next month",
            )
        )
    }

    @Test
    fun pinnedMemoryRanksAboveOtherwiseEqualMemory() {
        val now = 1_000_000L
        val normal = MemoryItem(
            id = "normal",
            kind = MemoryKind.FACT,
            text = "A fact",
            createdAt = now,
            updatedAt = now,
            importance = 3,
        )
        val pinned = normal.copy(id = "pinned", pinned = true)

        assertTrue(MemoryPolicy.score(pinned, now) > MemoryPolicy.score(normal, now))
    }

    @Test
    fun correction_replacesTextWhilePreservingRecordIdentityAndProvenance() {
        val original = MemoryItem(
            id = "stable-id",
            kind = MemoryKind.PREFERENCE,
            owner = MemoryOwner.USER,
            text = "Mohamed prefers blue",
            createdAt = 100L,
            updatedAt = 200L,
            importance = 5,
            confidence = 0.6,
            pinned = true,
            sourceCallId = "call-1",
        )

        val corrected = MemoryPolicy.corrected(
            item = original,
            replacementText = "  Mohamed prefers orange now  ",
            now = 500L,
        )!!

        assertEquals("stable-id", corrected.id)
        assertEquals("Mohamed prefers orange now", corrected.text)
        assertEquals(100L, corrected.createdAt)
        assertEquals(500L, corrected.updatedAt)
        assertEquals(1.0, corrected.confidence, 0.0)
        assertEquals(5, corrected.importance)
        assertTrue(corrected.pinned)
        assertEquals("call-1", corrected.sourceCallId)
        assertNotEquals(original.text, corrected.text)
    }

    @Test
    fun correction_rejectsBlankReplacement() {
        val item = MemoryItem(
            id = "id",
            kind = MemoryKind.FACT,
            text = "Existing fact",
            createdAt = 100L,
        )

        assertTrue(MemoryPolicy.corrected(item, "   ", 200L) == null)
    }
}
