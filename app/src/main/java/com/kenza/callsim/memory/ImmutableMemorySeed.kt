package com.kenza.callsim.memory

/**
 * Built-in continuity that must survive user edits, clear-memory actions and app
 * data resets. These are not extracted guesses. They are factual history the
 * owner asked to bake into Kenza's memory.
 */
object ImmutableMemorySeed {
    private const val CREATED_AT = 1_735_689_600_000L // 2025-01-01 UTC

    const val FUTURE_GOALS =
        "Getting married, buying a house together, traveling the world, buying a house for our parents in Morocco, " +
            "having at least 4 kids, and raising them well, teaching them their roots and Moroccan culture."

    private const val RELATIONSHIP_HISTORY =
        "Mohamed and Kenza have serious shared history and long-term emotional context. Their future plans are " +
            "part of the relationship continuity and should be treated as stable history during calls."

    private const val BOUNDARIES_CONTEXT =
        "Locked core memory: these relationship-history facts are built into the app and should not be erased, " +
            "dismissed, or rewritten by a temporary mood, argument, reset, or memory cleanup. Do not invent extra " +
            "sensitive details beyond the locked facts."

    private const val ID_SHARED_FUTURE_FULL = "core_kenza_shared_future_full"
    private const val ID_GETTING_MARRIED = "core_kenza_goal_getting_married"
    private const val ID_HOUSE_TOGETHER = "core_kenza_goal_house_together"
    private const val ID_TRAVEL_WORLD = "core_kenza_goal_travel_world"
    private const val ID_PARENTS_MOROCCO = "core_kenza_goal_parents_morocco"
    private const val ID_FOUR_KIDS = "core_kenza_goal_four_kids"
    private const val ID_MOROCCAN_ROOTS = "core_kenza_goal_moroccan_roots"
    private const val ID_LOCKED_HISTORY = "core_kenza_locked_history_rule"

    private val lockedIds = setOf(
        ID_SHARED_FUTURE_FULL,
        ID_GETTING_MARRIED,
        ID_HOUSE_TOGETHER,
        ID_TRAVEL_WORLD,
        ID_PARENTS_MOROCCO,
        ID_FOUR_KIDS,
        ID_MOROCCAN_ROOTS,
        ID_LOCKED_HISTORY,
    )

    val lockedItems: List<MemoryItem> = listOf(
        lockedItem(
            id = ID_SHARED_FUTURE_FULL,
            kind = MemoryKind.RELATIONSHIP,
            text = "Mohamed and Kenza's locked shared future/history: $FUTURE_GOALS",
        ),
        lockedItem(
            id = ID_GETTING_MARRIED,
            kind = MemoryKind.GOAL,
            text = "Mohamed and Kenza want to get married.",
        ),
        lockedItem(
            id = ID_HOUSE_TOGETHER,
            kind = MemoryKind.GOAL,
            text = "Mohamed and Kenza want to buy a house together.",
        ),
        lockedItem(
            id = ID_TRAVEL_WORLD,
            kind = MemoryKind.GOAL,
            text = "Mohamed and Kenza want to travel the world together.",
        ),
        lockedItem(
            id = ID_PARENTS_MOROCCO,
            kind = MemoryKind.GOAL,
            text = "Mohamed and Kenza want to buy a house for their parents in Morocco.",
        ),
        lockedItem(
            id = ID_FOUR_KIDS,
            kind = MemoryKind.GOAL,
            text = "Mohamed and Kenza want to have at least 4 kids.",
        ),
        lockedItem(
            id = ID_MOROCCAN_ROOTS,
            kind = MemoryKind.GOAL,
            text = "Mohamed and Kenza want to raise their children well and teach them their roots and Moroccan culture.",
        ),
        lockedItem(
            id = ID_LOCKED_HISTORY,
            kind = MemoryKind.FACT,
            text = "These shared-history memories are locked factual continuity for the Kenza app and must remain available after memory cleanup or app data reset.",
        ),
    )

    fun isLockedMemoryId(id: String): Boolean = id in lockedIds

    fun apply(snapshot: MemorySnapshot): MemorySnapshot {
        val unlockedItems = snapshot.items.filterNot { item ->
            isLockedMemoryId(item.id) || lockedItems.any { locked -> MemoryPolicy.isNearDuplicate(locked.text, item.text) }
        }
        return snapshot.copy(
            items = lockedItems + unlockedItems,
            profiles = snapshot.profiles.withLockedContinuity(),
        )
    }

    private fun PersonalityProfiles.withLockedContinuity(): PersonalityProfiles = copy(
        relationshipProfile = relationshipProfile.mergeLockedText(RELATIONSHIP_HISTORY),
        ambitionsAndGoals = ambitionsAndGoals.mergeLockedText(FUTURE_GOALS),
        boundariesAndContext = boundariesAndContext.mergeLockedText(BOUNDARIES_CONTEXT),
    )

    private fun String.mergeLockedText(locked: String): String {
        val clean = trim()
        return when {
            clean.isBlank() -> locked
            clean.contains(locked) -> clean
            else -> "$clean\n\n$locked"
        }
    }

    private fun lockedItem(id: String, kind: MemoryKind, text: String): MemoryItem = MemoryItem(
        id = id,
        kind = kind,
        owner = MemoryOwner.SHARED,
        text = text,
        createdAt = CREATED_AT,
        updatedAt = CREATED_AT,
        importance = 5,
        confidence = 1.0,
        pinned = true,
    )
}
