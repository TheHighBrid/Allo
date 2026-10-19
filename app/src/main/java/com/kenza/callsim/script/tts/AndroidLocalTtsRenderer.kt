package com.kenza.callsim.script.tts

import android.content.Context
import android.media.MediaMetadataRetriever
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.util.Locale
import kotlin.coroutines.resume

/**
 * Free, device-local Script Studio renderer using Android's installed TextToSpeech engine.
 * It is a preview renderer, not a cloned-voice provider. Each prepared speech block is synthesized
 * separately so listener-pause metadata remains exact in the playback timeline.
 */
class AndroidLocalTtsRenderer(context: Context) : TtsRenderer, TtsArtifactCleaner {
    private val appContext = context.applicationContext

    override suspend fun render(request: TtsRenderRequest): Result<TtsRenderResult> {
        val outputDir = File(appContext.cacheDir, "script_tts/${safeProjectId(request.projectId)}")
        val created = mutableListOf<File>()
        var tts: TextToSpeech? = null

        return try {
            outputDir.deleteRecursively()
            outputDir.mkdirs()

            tts = createEngine()
            configureLanguage(tts, request.language)

            val rendered = mutableListOf<TtsRenderedSegment>()
            request.segments.sortedBy { it.order }.forEach { segment ->
                segment.blocks.forEachIndexed { blockIndex, block ->
                    val file = File(outputDir, "%03d_%03d.wav".format(segment.order, blockIndex))
                    synthesizeBlock(tts, block.spokenText, file, "${segment.id}:$blockIndex")
                    created += file
                    rendered += TtsRenderedSegment(
                        segmentId = "${segment.id}:$blockIndex",
                        durationMs = mediaDurationMs(file),
                        artifactRef = file.absolutePath,
                        pauseBeforeMs = block.pauseBeforeMs,
                        performanceDirections = block.performanceDirections,
                    )
                }
            }

            Result.success(TtsRenderResult(projectId = request.projectId, segments = rendered))
        } catch (cancelled: CancellationException) {
            created.forEach(File::delete)
            outputDir.deleteRecursively()
            throw cancelled
        } catch (error: Throwable) {
            created.forEach(File::delete)
            outputDir.deleteRecursively()
            Result.failure(error)
        } finally {
            runCatching { tts?.stop() }
            runCatching { tts?.shutdown() }
        }
    }

    override fun cleanup(result: TtsRenderResult) {
        result.segments.forEach { clip -> runCatching { File(clip.artifactRef).delete() } }
        File(appContext.cacheDir, "script_tts/${safeProjectId(result.projectId)}").deleteRecursively()
    }

    private suspend fun createEngine(): TextToSpeech {
        val ready = CompletableDeferred<Int>()
        lateinit var engine: TextToSpeech
        engine = TextToSpeech(appContext) { status -> ready.complete(status) }
        try {
            if (ready.await() != TextToSpeech.SUCCESS) {
                engine.shutdown()
                error("Android text-to-speech is unavailable on this device.")
            }
            return engine
        } catch (cancelled: CancellationException) {
            engine.shutdown()
            throw cancelled
        }
    }

    private fun configureLanguage(tts: TextToSpeech, requested: String) {
        val locale = resolveLocale(requested)
        val status = tts.setLanguage(locale)
        if (status == TextToSpeech.LANG_MISSING_DATA || status == TextToSpeech.LANG_NOT_SUPPORTED) {
            error("The installed text-to-speech engine does not support ${requested.trim().ifBlank { "this language" }}.")
        }
    }

    private suspend fun synthesizeBlock(
        tts: TextToSpeech,
        text: String,
        file: File,
        utteranceId: String,
    ) = suspendCancellableCoroutine<Unit> { continuation ->
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) = Unit
            override fun onDone(id: String?) {
                if (id == utteranceId && continuation.isActive) continuation.resume(Unit)
            }
            @Deprecated("Deprecated in Android API")
            override fun onError(id: String?) {
                if (id == utteranceId && continuation.isActive) {
                    continuation.resumeWith(Result.failure(IllegalStateException("Local TTS could not render this script block.")))
                }
            }
            override fun onError(id: String?, errorCode: Int) {
                if (id == utteranceId && continuation.isActive) {
                    continuation.resumeWith(Result.failure(IllegalStateException("Local TTS render failed (code $errorCode).")))
                }
            }
        })

        val status = tts.synthesizeToFile(
            text,
            Bundle(),
            file,
            utteranceId,
        )
        if (status != TextToSpeech.SUCCESS && continuation.isActive) {
            continuation.resumeWith(Result.failure(IllegalStateException("Local TTS rejected the render request.")))
        }
        continuation.invokeOnCancellation { tts.stop() }
    }

    private fun mediaDurationMs(file: File): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        } catch (_: Throwable) {
            0L
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun resolveLocale(requested: String): Locale {
        val clean = requested.trim()
        if (clean.isBlank()) return Locale.getDefault()
        Locale.getAvailableLocales().firstOrNull {
            it.displayLanguage.equals(clean, ignoreCase = true) ||
                it.language.equals(clean, ignoreCase = true) ||
                it.toLanguageTag().equals(clean, ignoreCase = true)
        }?.let { return it }
        return Locale.forLanguageTag(clean).takeIf { it.language.isNotBlank() } ?: Locale.getDefault()
    }

    private fun safeProjectId(id: String): String = id.replace(Regex("[^A-Za-z0-9._-]"), "_").take(100)
}
