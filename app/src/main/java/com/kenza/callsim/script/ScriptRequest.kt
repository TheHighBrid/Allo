package com.kenza.callsim.script

/** The high-level conversational shape requested for a generated script. */
enum class ScriptMode {
    CASUAL_DAILY,
    BEFORE_SLEEP,
    MISSING_YOU,
    PLAYFUL,
    SUPPORTIVE,
    RELATIONSHIP_CHECK_IN,
    STORYTELLING,
    FUTURE_PLANNING,
    AFTER_ARGUMENT,
    SURPRISE,
    CUSTOM,
}

enum class IntensityLevel { NONE, LOW, MODERATE, HIGH }

/**
 * Provider-independent Script Studio request.
 *
 * Defaults keep the existing MVP call sites source-compatible while giving the domain layer the
 * complete set of controls required by the Script Studio specification. UI support can be added
 * incrementally without changing provider contracts again.
 */
data class ScriptRequest(
    val requestedMinutes: Int,
    val mode: ScriptMode = ScriptMode.CASUAL_DAILY,
    val language: String = "English",
    val callReason: String? = null,
    val timeOfDay: String? = null,
    val seasonOrDate: String? = null,
    val kenzaLocation: String? = null,
    val listenerLocation: String? = null,
    val relationshipStage: String? = null,
    val relationshipMood: String? = null,
    val mainTopics: List<String> = emptyList(),
    val recentEvents: List<String> = emptyList(),
    val selectedMemoryIds: List<String> = emptyList(),
    val currentProblems: List<String> = emptyList(),
    val futurePlans: List<String> = emptyList(),
    val kenzaMood: String? = null,
    val listenerLikelyMood: String? = null,
    val affection: IntensityLevel = IntensityLevel.MODERATE,
    val humor: IntensityLevel = IntensityLevel.MODERATE,
    val flirtation: IntensityLevel = IntensityLevel.LOW,
    val boundaries: List<String> = emptyList(),
    val endingStyle: String? = null,
    val customInstructions: String? = null,
) {
    /** Text used only to retrieve relevant memories. It is never persisted separately. */
    fun memoryQueryText(): String = buildList {
        add(mode.name.replace('_', ' '))
        callReason?.let(::add)
        mainTopics.forEach(::add)
        recentEvents.forEach(::add)
        currentProblems.forEach(::add)
        futurePlans.forEach(::add)
        relationshipMood?.let(::add)
        kenzaMood?.let(::add)
    }.joinToString(" ")
}
