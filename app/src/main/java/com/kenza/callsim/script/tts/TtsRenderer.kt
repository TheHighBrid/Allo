package com.kenza.callsim.script.tts

/** One provider-ready unit of speech plus non-spoken production metadata. */
data class TtsSegment(
    val id: String,
    val order: Int,
    val spokenText: String,
    val pauseBeforeMs: Long = 0,
    val performanceDirections: List<String> = emptyList(),
    /** Context for a provider/renderer, never text that should be spoken aloud. */
    val continuityContext: String = "",
)

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
 * Renderers receive already-cleaned speech. Bracketed performance directions and pauses are
 * metadata in [TtsSegment], so an adapter can interpret or ignore them without ever speaking them
 * by accident. Cancellation is cooperative through coroutine cancellation; no provider-specific
 * cancellation primitive leaks into callers.
 */
interface TtsRenderer {
    suspend fun render(request: TtsRenderRequest): Result<TtsRenderResult>
}
