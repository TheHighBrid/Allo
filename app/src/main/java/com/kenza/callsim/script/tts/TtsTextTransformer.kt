package com.kenza.callsim.script.tts

/** Parsed speech block with directions separated from audible text. */
data class TtsPreparedBlock(
    val spokenText: String,
    val pauseBeforeMs: Long = 0,
    val performanceDirections: List<String> = emptyList(),
)

/**
 * Converts Script Studio production notation into provider-safe TTS blocks.
 *
 * Pause directions become silence metadata. Other bracketed directions become performance
 * metadata. Neither category remains in [TtsPreparedBlock.spokenText], preventing a TTS provider
 * from literally saying "soft laugh" or "listening pause five seconds".
 */
object TtsTextTransformer {

    fun transform(scriptText: String): List<TtsPreparedBlock> {
        if (scriptText.isBlank()) return emptyList()
        val output = mutableListOf<TtsPreparedBlock>()
        var pendingPauseMs = 0L
        val pendingPerformance = mutableListOf<String>()

        scriptText.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isBlank()) return@forEach

            val directionOnly = DIRECTION_ONLY.matchEntire(line)?.groupValues?.get(1)?.trim()
            if (directionOnly != null) {
                val pause = pauseMs(directionOnly)
                if (pause != null) pendingPauseMs += pause
                else pendingPerformance += directionOnly
                return@forEach
            }

            var spoken = line
            INLINE_DIRECTION.findAll(line).forEach { match ->
                val direction = match.groupValues[1].trim()
                val pause = pauseMs(direction)
                if (pause != null) pendingPauseMs += pause
                else pendingPerformance += direction
            }
            spoken = INLINE_DIRECTION.replace(spoken, " ")
                .replace(Regex("\\s+"), " ")
                .trim()
            if (spoken.isNotBlank()) {
                output += TtsPreparedBlock(
                    spokenText = spoken,
                    pauseBeforeMs = pendingPauseMs,
                    performanceDirections = pendingPerformance.toList(),
                )
                pendingPauseMs = 0L
                pendingPerformance.clear()
            }
        }

        return output
    }

    internal fun pauseMs(direction: String): Long? {
        val normalized = direction.lowercase().trim()
        QUANTIFIED_PAUSE.matchEntire(normalized)?.let { match ->
            val seconds = match.groupValues[1].toDoubleOrNull() ?: return null
            return (seconds * 1_000.0).toLong().coerceIn(0L, MAX_PAUSE_MS)
        }
        return when (normalized) {
            "brief silence", "brief pause" -> 1_000L
            "longer listening pause" -> 8_000L
            else -> null
        }
    }

    private const val MAX_PAUSE_MS = 30_000L
    private val DIRECTION_ONLY = Regex("^\\[([^]]+)]$")
    private val INLINE_DIRECTION = Regex("\\[([^]]+)]")
    private val QUANTIFIED_PAUSE = Regex(
        "(?:listening\\s+)?pause\\s+(\\d+(?:\\.\\d+)?)\\s+seconds?",
        RegexOption.IGNORE_CASE,
    )
}
