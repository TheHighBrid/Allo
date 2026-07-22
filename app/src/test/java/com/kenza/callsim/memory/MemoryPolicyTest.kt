package com.kenza.callsim.memory

import org.junit.Assert.assertFalse
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
}
