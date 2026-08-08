package com.kenza.callsim.initiative

import com.kenza.callsim.memory.CallSummary
import com.kenza.callsim.memory.MemoryItem
import com.kenza.callsim.memory.MemoryKind
import com.kenza.callsim.memory.MemoryOwner
import com.kenza.callsim.memory.MemoryPolicy
import com.kenza.callsim.memory.MemorySnapshot
import java.util.Calendar
import kotlin.math.roundToInt

enum class InitiativeKind {
    FOLLOW_UP,
    UNRESOLVED_TOPIC,
    OPEN_PLAN,
    EMOTIONAL_CHECK_IN,
    KENZA_MEMORY,
    SHARED_MEMORY,
    USER_MEMORY,
    SOCIAL_CURIOSITY,
}

data class InitiativeSeed(
    val token: String,
    val kind: InitiativeKind,
    val cue: String,
    val relevance: Int,
)

data class InitiativeHistory(
    val lastInitiatedAt: Long = 0L,
    val recentTokens: Set<String> = emptySet(),
)

data class InitiativeDecision(
    val shouldCall: Boolean,
    val score: Int,
    val seed: InitiativeSeed?,
    val reason: String,
)

/**
 * Pure decision engine for Kenza's conversational agency.
 *
 * It deliberately does not roll random dice. A call is earned from relationship
 * continuity, time-of-day fit, unresolved threads, plans, mood, memory ownership,
 * recency and interruption cost. Small deterministic tie-breakers keep repeated
 * evaluations stable instead of making the phone ring because a RNG happened to
 * say yes.
 */
object InitiativeEngine {

    private const val HOUR_MS = 3_600_000L
    private const val DAY_MS = 86_400_000L
    private const val MIN_CALL_GAP_MS = 90 * 60_000L
    const val AUTONOMOUS_COOLDOWN_MS = 6 * HOUR_MS
    private const val CALL_THRESHOLD = 54

    fun evaluate(
        snapshot: MemorySnapshot,
        history: InitiativeHistory,
        now: Long,
    ): InitiativeDecision {
        val latestCall = snapshot.calls
            .asSequence()
            .filterNot { it.processing }
            .maxByOrNull { it.startedAt }
        val sinceLastCall = latestCall?.let { now - it.startedAt } ?: Long.MAX_VALUE
        val sinceAutonomous = if (history.lastInitiatedAt > 0L) now - history.lastInitiatedAt else Long.MAX_VALUE
        val urgent = hasDueAroundNow(snapshot.items, now)
        val hour = Calendar.getInstance().apply { timeInMillis = now }.get(Calendar.HOUR_OF_DAY)

        if (hour in 2..7 && !urgent) {
            return InitiativeDecision(false, 0, null, "quiet-hours interruption cost")
        }
        if (sinceAutonomous in 0 until AUTONOMOUS_COOLDOWN_MS) {
            return InitiativeDecision(false, 0, null, "autonomous-call cooldown")
        }
        if (sinceLastCall in 0 until MIN_CALL_GAP_MS) {
            return InitiativeDecision(false, 0, null, "you just talked")
        }

        val seed = bestSeed(snapshot, now, history.recentTokens)
            ?: return InitiativeDecision(false, 0, null, "no meaningful conversation seed")

        var score = seed.relevance
        score += timeFit(hour)
        score += relationshipGapScore(sinceLastCall)
        score += moodScore(latestCall)
        score -= sameDaySaturation(snapshot.calls, now)

        // Deterministic micro-adjustment prevents permanent ties without making
        // the decision stochastic. Same memory state + same time slot = same result.
        score += stableTieBreak(seed.token, now)
        score = score.coerceIn(0, 100)

        return InitiativeDecision(
            shouldCall = score >= CALL_THRESHOLD,
            score = score,
            seed = seed,
            reason = if (score >= CALL_THRESHOLD) "context is strong enough to initiate" else "worth remembering, not worth interrupting yet",
        )
    }

    fun bestSeed(
        snapshot: MemorySnapshot,
        now: Long,
        excludedTokens: Set<String> = emptySet(),
    ): InitiativeSeed? = candidates(snapshot, now)
        .filterNot { it.token in excludedTokens }
        .sortedWith(compareByDescending<InitiativeSeed> { it.relevance }.thenBy { it.token })
        .firstOrNull()
        ?: socialSeed(snapshot).takeIf { it.token !in excludedTokens }

    /** Resolve a previously selected token against fresh encrypted memory. */
    fun resolveCue(snapshot: MemorySnapshot, token: String, now: Long): String? =
        candidates(snapshot, now).firstOrNull { it.token == token }?.cue
            ?: socialSeed(snapshot).takeIf { it.token == token }?.cue

    /**
     * Next evaluation cadence. This is a check schedule, not a call schedule.
     * Calls still require [evaluate] to cross the contextual threshold.
     */
    fun nextEvaluationDelayMs(snapshot: MemorySnapshot, history: InitiativeHistory, now: Long): Long {
        val hour = Calendar.getInstance().apply { timeInMillis = now }.get(Calendar.HOUR_OF_DAY)
        if (hour in 1..7) {
            val wake = Calendar.getInstance().apply {
                timeInMillis = now
                set(Calendar.HOUR_OF_DAY, 8)
                set(Calendar.MINUTE, 30)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= now) add(Calendar.DAY_OF_YEAR, 1)
            }
            return (wake.timeInMillis - now).coerceAtLeast(30 * 60_000L)
        }

        if (history.lastInitiatedAt > 0L) {
            val remaining = AUTONOMOUS_COOLDOWN_MS - (now - history.lastInitiatedAt)
            if (remaining > 0L) return (remaining + 15 * 60_000L).coerceAtMost(8 * HOUR_MS)
        }

        val latest = snapshot.calls.filterNot { it.processing }.maxOfOrNull { it.startedAt }
        if (latest != null && now - latest < 2 * HOUR_MS) return 90 * 60_000L

        // Between 75 and 120 minutes, derived from current memory/call state.
        val stateKey = snapshot.updatedAt xor (latest ?: 0L) xor (now / HOUR_MS)
        val extraMinutes = ((stateKey and Long.MAX_VALUE) % 46L).toInt()
        return (75L + extraMinutes) * 60_000L
    }

    private fun candidates(snapshot: MemorySnapshot, now: Long): List<InitiativeSeed> {
        val out = mutableListOf<InitiativeSeed>()
        val calls = snapshot.calls.filterNot { it.processing }.sortedByDescending { it.startedAt }.take(5)

        calls.forEachIndexed { callIndex, call ->
            val ageDays = ((now - call.startedAt).coerceAtLeast(0) / DAY_MS.toDouble()).roundToInt()
            val recency = (10 - ageDays.coerceAtMost(10)).coerceAtLeast(0)
            call.followUp?.clean()?.takeIf { it.isNotBlank() }?.let { text ->
                out += InitiativeSeed(
                    token = token("follow", call.id, text),
                    kind = InitiativeKind.FOLLOW_UP,
                    cue = "You wanted to follow up on this from a recent conversation: $text. Bring it up naturally because you actually care what happened, not because a reminder told you to.",
                    relevance = 39 + recency - callIndex,
                )
            }
            call.unresolvedTopics.take(4).forEach { topic ->
                val clean = topic.clean()
                if (clean.isNotBlank()) {
                    out += InitiativeSeed(
                        token = token("unresolved", call.id, clean),
                        kind = InitiativeKind.UNRESOLVED_TOPIC,
                        cue = "A thread between you never really finished: $clean. Re-enter it in your own words, with a real opinion or question, rather than asking for a generic update.",
                        relevance = 35 + recency - callIndex,
                    )
                }
            }
            if (callIndex == 0 && call.mood.looksEmotionallyLoaded()) {
                out += InitiativeSeed(
                    token = "mood:${call.id}",
                    kind = InitiativeKind.EMOTIONAL_CHECK_IN,
                    cue = "The last conversation had this emotional tone: ${call.mood.clean()}. Check in without sounding clinical, alarmed, or like you're reading a mood label. You can also move to another topic if the moment has clearly passed.",
                    relevance = 38 + recency,
                )
            }
        }

        snapshot.items.asSequence()
            .filterNot { it.done }
            .sortedByDescending { MemoryPolicy.score(it, now) }
            .take(40)
            .forEach { item ->
                memorySeed(item, now)?.let(out::add)
            }

        return out
            .groupBy { it.token }
            .mapNotNull { (_, group) -> group.maxByOrNull { it.relevance } }
    }

    private fun memorySeed(item: MemoryItem, now: Long): InitiativeSeed? {
        val text = item.text.clean()
        if (text.isBlank()) return null
        val ageDays = ((now - item.updatedAt).coerceAtLeast(0) / DAY_MS.toDouble()).roundToInt()
        val recency = (6 - ageDays.coerceAtMost(6)).coerceAtLeast(0)
        val importance = item.importance.coerceIn(1, 5)

        if ((item.kind == MemoryKind.PLAN || item.kind == MemoryKind.GOAL) && !item.done) {
            val dueBoost = item.dueAt?.let { due ->
                val delta = kotlin.math.abs(due - now)
                when {
                    delta <= DAY_MS -> 16
                    delta <= 3 * DAY_MS -> 10
                    delta <= 7 * DAY_MS -> 5
                    else -> 0
                }
            } ?: 0
            return InitiativeSeed(
                token = "memory:${item.id}",
                kind = InitiativeKind.OPEN_PLAN,
                cue = "There is an open plan or goal in your shared continuity: $text. Use the timing and relationship context to decide whether to ask about it, challenge it, celebrate progress, or connect it to something else.",
                relevance = 23 + importance * 3 + recency + dueBoost,
            )
        }

        val (kind, ownerBoost, framing) = when (item.owner) {
            MemoryOwner.KENZA -> Triple(
                InitiativeKind.KENZA_MEMORY,
                12,
                "This is associated with your own established interests, opinions, or history: $text. You are allowed to bring it up because YOU find it relevant or interesting, not only because Mohamed mentioned it first."
            )
            MemoryOwner.SHARED -> Triple(
                InitiativeKind.SHARED_MEMORY,
                9,
                "This belongs to your shared history: $text. Let it spark a natural thought, callback, question, disagreement, joke, or new direction."
            )
            MemoryOwner.USER -> Triple(
                InitiativeKind.USER_MEMORY,
                3,
                "This is something meaningful about Mohamed: $text. Only use it if it creates a specific, timely conversation rather than a database-style check-in."
            )
        }
        return InitiativeSeed(
            token = "memory:${item.id}",
            kind = kind,
            cue = framing,
            relevance = 16 + importance * 3 + recency + ownerBoost + if (item.pinned) 5 else 0,
        )
    }

    private fun socialSeed(snapshot: MemorySnapshot): InitiativeSeed {
        val hasProfiles = snapshot.profiles.kenzaProfile.isNotBlank() ||
            snapshot.profiles.listenerProfile.isNotBlank() ||
            snapshot.profiles.relationshipProfile.isNotBlank()
        return InitiativeSeed(
            token = SOCIAL_TOKEN,
            kind = InitiativeKind.SOCIAL_CURIOSITY,
            cue = if (hasProfiles) {
                "Choose one specific subject YOU would plausibly want to talk about from the private personality, relationship, and memory briefing. It can be curious, funny, serious, opinionated, practical, nostalgic, creative, or completely new. Do not explain why you selected it."
            } else {
                "Start with a genuine curiosity, observation, opinion, or story-shaped question instead of a generic 'what are you doing?' opener. Pick something that can become a real conversation."
            },
            relevance = if (hasProfiles) 22 else 14,
        )
    }

    private fun hasDueAroundNow(items: List<MemoryItem>, now: Long): Boolean = items.any { item ->
        !item.done && (item.kind == MemoryKind.PLAN || item.kind == MemoryKind.GOAL) &&
            item.dueAt?.let { kotlin.math.abs(it - now) <= DAY_MS } == true
    }

    private fun timeFit(hour: Int): Int = when (hour) {
        in 8..10 -> 12
        in 11..16 -> 17
        in 17..20 -> 19
        in 21..23 -> 16
        0, 1 -> 5
        else -> -30
    }

    private fun relationshipGapScore(sinceLastCall: Long): Int = when {
        sinceLastCall == Long.MAX_VALUE -> 13
        sinceLastCall < 4 * HOUR_MS -> -9
        sinceLastCall < 12 * HOUR_MS -> -2
        sinceLastCall < DAY_MS -> 3
        sinceLastCall < 2 * DAY_MS -> 8
        sinceLastCall < 4 * DAY_MS -> 12
        else -> 16
    }

    private fun moodScore(call: CallSummary?): Int = if (call?.mood.looksEmotionallyLoaded()) 4 else 0

    private fun sameDaySaturation(calls: List<CallSummary>, now: Long): Int {
        val today = Calendar.getInstance().apply { timeInMillis = now }
        val count = calls.count { call ->
            val then = Calendar.getInstance().apply { timeInMillis = call.startedAt }
            today.get(Calendar.ERA) == then.get(Calendar.ERA) &&
                today.get(Calendar.YEAR) == then.get(Calendar.YEAR) &&
                today.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR)
        }
        return when {
            count >= 4 -> 28
            count == 3 -> 20
            count == 2 -> 11
            count == 1 -> 4
            else -> 0
        }
    }

    private fun stableTieBreak(token: String, now: Long): Int {
        val slot = now / (2 * HOUR_MS)
        return ((token.hashCode().toLong() xor slot) and 0x7fffffffL).rem(5L).toInt()
    }

    private fun token(prefix: String, id: String, text: String): String =
        "$prefix:$id:${text.hashCode().toUInt().toString(16)}"

    private fun String?.looksEmotionallyLoaded(): Boolean {
        val value = this?.lowercase().orEmpty()
        return listOf(
            "sad", "upset", "angry", "frustrat", "stressed", "anxious", "hurt",
            "excited", "happy", "proud", "emotional", "tense", "worried", "low"
        ).any(value::contains)
    }

    private fun String.clean(): String = replace(Regex("\\s+"), " ").trim().take(420)

    const val SOCIAL_TOKEN = "social:curiosity"
}
