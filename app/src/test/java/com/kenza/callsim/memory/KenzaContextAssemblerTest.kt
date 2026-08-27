package com.kenza.callsim.memory

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KenzaContextAssemblerTest {

    @Test
    fun `script query retrieves matching memory and excludes unrelated memory`() {
        val now = 1_800_000_000_000L
        val snapshot = MemorySnapshot(
            items = listOf(
                memory("work", "Mohamed is preparing a new Melato collection launch", now),
                memory("food", "Mohamed likes spicy ramen", now),
            ),
            profiles = PersonalityProfiles(
                kenzaProfile = "Warm, direct, playful",
                listenerProfile = "Runs Melato",
                relationshipProfile = "They speak often and plan together",
            ),
        )

        val context = KenzaContextAssembler.assemble(
            snapshot = snapshot,
            personaName = "Kenza",
            now = now,
            queryText = "Melato collection work launch",
        )

        assertEquals(listOf("work"), context.memoryIdsUsed)
        assertTrue(context.toPrompt().contains("Melato collection launch"))
        assertFalse(context.toPrompt().contains("spicy ramen"))
    }

    @Test
    fun `explicitly selected memory is retained even when query does not match`() {
        val now = 1_800_000_000_000L
        val snapshot = MemorySnapshot(
            items = listOf(
                memory("selected", "A deliberately selected shared memory", now),
                memory("other", "A football conversation", now),
            ),
        )

        val context = KenzaContextAssembler.assemble(
            snapshot = snapshot,
            personaName = "Kenza",
            now = now,
            queryText = "fashion design",
            selectedMemoryIds = setOf("selected"),
        )

        assertEquals(listOf("selected"), context.memoryIdsUsed)
    }

    @Test
    fun `live context without query keeps bounded highest-value memories`() {
        val now = 1_800_000_000_000L
        val snapshot = MemorySnapshot(
            items = (1..20).map { index ->
                memory(
                    id = index.toString(),
                    text = "Memory number $index",
                    now = now - index * 1_000L,
                    importance = if (index == 20) 5 else 3,
                )
            },
        )

        val context = KenzaContextAssembler.assemble(
            snapshot = snapshot,
            personaName = "Kenza",
            now = now,
            maxMemories = 5,
        )

        assertEquals(5, context.retrievedMemories.size)
        assertTrue(context.memoryIdsUsed.contains("20"))
    }

    private fun memory(
        id: String,
        text: String,
        now: Long,
        importance: Int = 4,
    ) = MemoryItem(
        id = id,
        kind = MemoryKind.FACT,
        text = text,
        createdAt = now,
        updatedAt = now,
        importance = importance,
        confidence = 1.0,
    )
}
