package com.kenza.callsim.script.tts

import java.util.UUID

/**
 * Splits a long script into bounded provider-ready units without breaking prepared blocks in half.
 * Each segment carries a short tail from the previous segment as non-spoken continuity context.
 */
object TtsSegmenter {

    fun segment(
        scriptText: String,
        maxSpokenChars: Int = DEFAULT_MAX_SPOKEN_CHARS,
        continuityChars: Int = DEFAULT_CONTINUITY_CHARS,
    ): List<TtsSegment> {
        require(maxSpokenChars >= 200) { "maxSpokenChars must be at least 200" }
        require(continuityChars >= 0) { "continuityChars must not be negative" }

        val blocks = TtsTextTransformer.transform(scriptText)
        if (blocks.isEmpty()) return emptyList()

        val segments = mutableListOf<TtsSegment>()
        val current = mutableListOf<TtsPreparedBlock>()
        var currentChars = 0
        var previousTail = ""

        fun flush() {
            if (current.isEmpty()) return
            val frozen = current.toList()
            val spoken = frozen.joinToString("\n\n") { it.spokenText }.trim()
            segments += TtsSegment(
                id = UUID.randomUUID().toString(),
                order = segments.size,
                blocks = frozen,
                continuityContext = previousTail,
            )
            previousTail = spoken.takeLast(continuityChars)
            current.clear()
            currentChars = 0
        }

        blocks.forEach { block ->
            val separatorChars = if (current.isEmpty()) 0 else 2
            val projected = currentChars + separatorChars + block.spokenText.length
            if (current.isNotEmpty() && projected > maxSpokenChars) flush()
            current += block
            currentChars += (if (current.size == 1) 0 else 2) + block.spokenText.length
        }
        flush()
        return segments
    }

    private const val DEFAULT_MAX_SPOKEN_CHARS = 3_200
    private const val DEFAULT_CONTINUITY_CHARS = 320
}
