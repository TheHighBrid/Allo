package com.kenza.callsim.script.tts

/** One provider-ready unit of speech plus ordered, non-spoken production metadata. */
data class TtsSegment(
    val id: String,
    val order: Int,
    val blocks: List<TtsPreparedBlock>,
    /** Context for a provider/renderer, never text that should be spoken aloud. */
    val continuityContext: String = "",
) {
    val spokenText: String get() = blocks.joinToString("\n\n") { it.spokenText }.trim()
    val pauseBeforeMs: Long get() = blocks.firstOrNull()?.pauseBeforeMs ?: 0L
    val performanceDirections: List<String>
        get() = blocks.flatMap { it.performanceDirections }.distinct()
}

/** Provider-independent request. No API key or provider-specific voice identifier belongs here. */
data class TtsRenderRequest(
    val projectId: String,
    val segments: List<TtsSegment>,
    val language: String,
    val voiceHint: String? = null,
)

/**
 * One rendered speech clip. Renderers may emit several clips for one logical segment so explicit
 * listener pauses survive as timeline metadata instead of being flattened into punctuation.
 */
data class TtsRenderedSegment(
    val segmentId: String,
    val durationMs: Long,
    val artifactRef: String,
    val pauseBeforeMs: Long = 0L,
    val performanceDirections: List<String> = emptyList(),
)

data class TtsRenderResult(
    val projectId: String,
    val segments: List<TtsRenderedSegment>,
) {
    val totalDurationMs: Long get() = segments.sumOf { it.pauseBeforeMs + it.durationMs }
}

/** Stable boundary for approved TTS implementations. */
interface TtsRenderer {
    suspend fun render(request: TtsRenderRequest): Result<TtsRenderResult>
}

/** Optional cleanup capability for renderers that create temporary local artifacts. */
interface TtsArtifactCleaner {
    fun cleanup(result: TtsRenderResult)
}
