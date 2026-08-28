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

data class TtsRenderedSegment(
    val segmentId: String,
    val durationMs: Long,
    /** Provider adapter owns the meaning of this opaque local artifact reference. */
    val artifactRef: String,
)

data class TtsRenderResult(
    val projectId: String,
    val segments: List<TtsRenderedSegment>,
) {
    val totalDurationMs: Long get() = segments.sumOf { it.durationMs }
}

/**
 * Stable boundary for future approved TTS implementations.
 *
 * Renderers receive already-cleaned speech blocks. Bracketed performance directions and pauses are
 * metadata, so an adapter can interpret or ignore them without ever speaking them by accident.
 * Cancellation is cooperative through coroutine cancellation; provider-specific details stay out
 * of callers.
 */
interface TtsRenderer {
    suspend fun render(request: TtsRenderRequest): Result<TtsRenderResult>
}
