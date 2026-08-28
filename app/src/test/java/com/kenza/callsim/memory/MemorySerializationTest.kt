package com.kenza.callsim.memory

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class MemorySerializationTest {

    @Test
    fun `memory snapshot round trip preserves durable and pending metadata`() {
        val durable = MemoryItem(
            id = "durable-1",
            kind = MemoryKind.PLAN,
            owner = MemoryOwner.SHARED,
            text = "Book the Montreal weekend.",
            createdAt = 1_000L,
            updatedAt = 2_000L,
            importance = 5,
            confidence = 0.93,
            dueAt = 9_000L,
            done = false,
            pinned = true,
            sourceCallId = "call-1",
        )
        val candidate = MemoryItem(
            id = "candidate-1",
            kind = MemoryKind.PREFERENCE,
            owner = MemoryOwner.USER,
            text = "Prefers the quieter restaurant table.",
            createdAt = 3_000L,
            updatedAt = 3_000L,
            importance = 4,
            confidence = 0.78,
            sourceCallId = "call-1",
        )
        val call = CallSummary(
            id = "call-1",
            startedAt = 4_000L,
            endedAt = 64_000L,
            summary = "They planned a Montreal weekend and talked about dinner.",
            mood = "warm",
            highlights = listOf("Weekend planning", "Restaurant preference"),
            unresolvedTopics = listOf("Choose hotel"),
            followUp = "Ask which hotel area feels best.",
            memoryIds = listOf("durable-1"),
            candidateMemories = listOf(candidate),
            processing = false,
            processingError = null,
        )
        val profiles = PersonalityProfiles(
            kenzaProfile = "Warm and direct.",
            listenerProfile = "Runs Melato.",
            relationshipProfile = "Long-term couple.",
            ambitionsAndGoals = "Travel more.",
            boundariesAndContext = "Do not invent private facts.",
            updatedAt = 5_000L,
        )
        val original = MemorySnapshot(
            items = listOf(durable),
            calls = listOf(call),
            profiles = profiles,
            updatedAt = 6_000L,
        )

        val encoded = original.toJson().toString()
        val restored = MemorySnapshot.fromJson(JSONObject(encoded))

        assertEquals(original, restored)
    }
}
