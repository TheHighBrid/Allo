package com.kenza.callsim.memory

import android.util.Log
import java.util.UUID

/**
 * Condenses old, low-priority ("cold") memories that fall outside the store's retention
 * window into one [MemoryKind.NARRATIVE] anchor, so pruning loses less history.
 *
 * The language-model call is injected as [summarize]. No summarizer is wired yet, so the
 * default compressor is deliberately inert: [isEnabled] is false, [compress] returns null,
 * no network request is made, and no placeholder text is ever written into memory. Pruning
 * therefore behaves exactly as it did before compression existed until a real summarizer
 * (for example the Gemini client used by [MemoryExtractor]) is supplied.
 */
class MemoryCompressor(
    private val summarize: (suspend (prompt: String) -> String?)? = null,
    private val clock: () -> Long = System::currentTimeMillis,
) {

    val isEnabled: Boolean get() = summarize != null

    suspend fun compress(coldItems: List<MemoryItem>): MemoryItem? {
        val summarizer = summarize ?: return null
        if (coldItems.isEmpty()) return null

        return try {
            val summary = summarizer(buildPrompt(coldItems))
                ?.trim()
                ?.replace(Regex("\\s+"), " ")
                ?.take(MAX_NARRATIVE_CHARS)
                ?.takeIf { it.isNotBlank() }
                ?: return null
            val now = clock()
            MemoryItem(
                id = UUID.randomUUID().toString(),
                kind = MemoryKind.NARRATIVE,
                text = summary,
                createdAt = now,
                updatedAt = now,
                importance = 4, // Narratives are high-value anchors.
                confidence = 0.8,
                owner = MemoryOwner.SHARED,
            )
        } catch (e: Exception) {
            // Never log memory text; the exception type is enough to diagnose.
            Log.w(TAG, "Compression failed: ${e.javaClass.simpleName}")
            null
        }
    }

    companion object {
        private const val TAG = "MemoryCompressor"

        /** Matches the per-item cap enforced by [MemoryStore.addAll]. */
        const val MAX_NARRATIVE_CHARS = 500

        fun buildPrompt(coldItems: List<MemoryItem>): String {
            val context = coldItems.joinToString("\n") { "- ${it.text} (Importance: ${it.importance})" }
            return """
                The following are old, low-priority memories from a user's history.
                Synthesize them into a single, concise historical narrative paragraph
                that preserves key facts, emotional arcs, and relationship milestones.
                Avoid listing; write as a coherent summary. Never invent details.

                Memories:
                $context
            """.trimIndent()
        }
    }
}
