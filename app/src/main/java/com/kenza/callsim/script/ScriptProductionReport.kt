package com.kenza.callsim.script

import com.kenza.callsim.memory.KenzaContext

/** Review-only memory suggestion derived from explicit request input, never generated fiction. */
data class ScriptMemoryCandidate(
    val text: String,
    val category: String,
    val confidence: Double,
    val shouldStore: Boolean = false,
    val reason: String,
)

data class ScriptProductionReport(
    val continuityNotes: List<String> = emptyList(),
    val unresolvedTopics: List<String> = emptyList(),
    val pronunciationNotes: List<String> = emptyList(),
    val candidateMemories: List<ScriptMemoryCandidate> = emptyList(),
)

/**
 * Builds production metadata from user-supplied request data and retrieved context only.
 * Generated script prose is deliberately not accepted as an input, preventing fictional details
 * introduced for performance from silently becoming factual continuity or memory candidates.
 */
object ScriptProductionReportBuilder {

    fun build(request: ScriptRequest, context: KenzaContext): ScriptProductionReport {
        val continuity = buildList {
            request.mainTopics.clean(8).forEach { add("Keep continuity around: $it") }
            request.relationshipMood.cleanValue()?.let { add("Relationship mood: $it") }
            request.kenzaMood.cleanValue()?.let { add("Kenza mood: $it") }
            request.endingStyle.cleanValue()?.let { add("Ending direction: $it") }
            if (context.memoryIdsUsed.isNotEmpty()) {
                add("Retrieved ${context.memoryIdsUsed.size} reviewed durable memory item${if (context.memoryIdsUsed.size == 1) "" else "s"}.")
            }
        }.distinct().take(12)

        val unresolved = (request.currentProblems + request.futurePlans)
            .clean(10)

        val candidateMemories = buildList {
            request.recentEvents.clean(5).forEach { event ->
                add(
                    ScriptMemoryCandidate(
                        text = event,
                        category = "recent_event",
                        confidence = 1.0,
                        shouldStore = false,
                        reason = "Explicitly supplied in the script request; review before durable storage.",
                    )
                )
            }
            request.futurePlans.clean(5).forEach { plan ->
                add(
                    ScriptMemoryCandidate(
                        text = plan,
                        category = "future_plan",
                        confidence = 1.0,
                        shouldStore = false,
                        reason = "Explicitly supplied in the script request; review before durable storage.",
                    )
                )
            }
        }.distinctBy { it.category to it.text }.take(10)

        return ScriptProductionReport(
            continuityNotes = continuity,
            unresolvedTopics = unresolved,
            pronunciationNotes = pronunciationReviewTerms(request),
            candidateMemories = candidateMemories,
        )
    }

    /** Flags only terms that are plausibly worth a human pronunciation check; it never invents a phonetic spelling. */
    private fun pronunciationReviewTerms(request: ScriptRequest): List<String> {
        val source = buildList {
            addAll(request.mainTopics)
            addAll(request.recentEvents)
            request.kenzaLocation?.let(::add)
            request.listenerLocation?.let(::add)
        }.joinToString(" ")

        return TOKEN.findAll(source)
            .map { it.value.trim() }
            .filter { token ->
                token.length >= 3 && (
                    token.any { it.code > 127 } ||
                        token.count(Char::isUpperCase) >= 2 ||
                        token.contains('-') || token.contains('\'')
                    )
            }
            .distinctBy { it.lowercase() }
            .take(12)
            .map { "Review pronunciation: $it" }
            .toList()
    }

    private fun List<String>.clean(limit: Int): List<String> = asSequence()
        .map { it.trim().replace(Regex("\\s+"), " ") }
        .filter { it.isNotBlank() }
        .distinct()
        .take(limit)
        .toList()

    private fun String?.cleanValue(): String? = this
        ?.trim()
        ?.replace(Regex("\\s+"), " ")
        ?.takeIf { it.isNotBlank() }

    private val TOKEN = Regex("[\\p{L}\\p{N}'’-]+")
}
