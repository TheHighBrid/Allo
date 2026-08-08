package com.kenza.callsim.initiative

import com.kenza.callsim.memory.CallSummary
import com.kenza.callsim.memory.MemoryItem
import com.kenza.callsim.memory.MemoryKind
import com.kenza.callsim.memory.MemoryOwner
import com.kenza.callsim.memory.MemorySnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class InitiativeEngineTest {

    @Test
    fun recentFollowUpCanEarnAnEveningCall() {
        val now = at(hour = 19, day = 7)
        val previous = CallSummary(
            id = "call-1",
            startedAt = now - 2 * DAY_MS,
            endedAt = now - 2 * DAY_MS + 20 * 60_000L,
            summary = "Talked about an upcoming decision.",
            followUp = "Ask what happened with the decision he was making on Wednesday.",
        )

        val decision = InitiativeEngine.evaluate(
            snapshot = MemorySnapshot(calls = listOf(previous)),
            history = InitiativeHistory(),
            now = now,
        )

        assertTrue(decision.shouldCall)
        assertEquals(InitiativeKind.FOLLOW_UP, decision.seed?.kind)
        assertTrue(decision.score >= 54)
    }

    @Test
    fun quietHoursBlockNonUrgentAutonomousCalls() {
        val now = at(hour = 3, day = 7)
        val previous = CallSummary(
            id = "call-2",
            startedAt = now - 3 * DAY_MS,
            endedAt = now - 3 * DAY_MS + 10 * 60_000L,
            summary = "A conversation with a strong loose end.",
            followUp = "Ask how the launch went.",
        )

        val decision = InitiativeEngine.evaluate(
            MemorySnapshot(calls = listOf(previous)),
            InitiativeHistory(),
            now,
        )

        assertFalse(decision.shouldCall)
        assertEquals("quiet-hours interruption cost", decision.reason)
    }

    @Test
    fun autonomousCooldownPreventsCallSpam() {
        val now = at(hour = 18, day = 7)
        val memory = MemoryItem(
            id = "shared-1",
            kind = MemoryKind.RELATIONSHIP,
            owner = MemoryOwner.SHARED,
            text = "They have an unfinished debate they both enjoyed.",
            createdAt = now - DAY_MS,
            updatedAt = now - DAY_MS,
            importance = 5,
        )

        val decision = InitiativeEngine.evaluate(
            snapshot = MemorySnapshot(items = listOf(memory)),
            history = InitiativeHistory(lastInitiatedAt = now - 2 * 60 * 60_000L),
            now = now,
        )

        assertFalse(decision.shouldCall)
        assertEquals("autonomous-call cooldown", decision.reason)
    }

    @Test
    fun kenzaOwnedMemoryBeatsEquivalentUserFact() {
        val now = at(hour = 18, day = 7)
        val userFact = MemoryItem(
            id = "user",
            kind = MemoryKind.PREFERENCE,
            owner = MemoryOwner.USER,
            text = "Mohamed likes a particular film director.",
            createdAt = now,
            updatedAt = now,
            importance = 4,
        )
        val kenzaFact = userFact.copy(
            id = "kenza",
            owner = MemoryOwner.KENZA,
            text = "Kenza has a strong opinion about a film they discussed.",
        )

        val seed = InitiativeEngine.bestSeed(
            MemorySnapshot(items = listOf(userFact, kenzaFact)),
            now,
        )

        assertNotNull(seed)
        assertEquals("memory:kenza", seed?.token)
        assertEquals(InitiativeKind.KENZA_MEMORY, seed?.kind)
    }

    @Test
    fun recentlyUsedTopicFallsBackToAnotherSeed() {
        val now = at(hour = 18, day = 7)
        val first = MemoryItem(
            id = "first",
            kind = MemoryKind.PLAN,
            owner = MemoryOwner.SHARED,
            text = "Follow up on tomorrow's plan.",
            createdAt = now,
            updatedAt = now,
            importance = 5,
            dueAt = now + DAY_MS,
        )
        val second = MemoryItem(
            id = "second",
            kind = MemoryKind.RELATIONSHIP,
            owner = MemoryOwner.KENZA,
            text = "Bring back a funny disagreement from earlier in the week.",
            createdAt = now,
            updatedAt = now,
            importance = 5,
        )

        val initial = InitiativeEngine.bestSeed(MemorySnapshot(items = listOf(first, second)), now)
        val next = InitiativeEngine.bestSeed(
            MemorySnapshot(items = listOf(first, second)),
            now,
            excludedTokens = setOf(requireNotNull(initial).token),
        )

        assertNotNull(next)
        assertTrue(next?.token != initial.token)
    }

    private fun at(hour: Int, day: Int): Long = Calendar.getInstance().apply {
        set(2026, Calendar.AUGUST, day, hour, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private companion object {
        const val DAY_MS = 86_400_000L
    }
}
