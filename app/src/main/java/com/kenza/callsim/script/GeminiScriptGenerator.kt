package com.kenza.callsim.script

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** Transport boundary that keeps Gemini provider behavior unit-testable. */
interface GeminiScriptTransport {
    suspend fun generate(model: String, apiKey: String, prompt: String): Result<String>
}

/**
 * Production text generator for Script Studio.
 *
 * The API key is used only while the request is in flight. It is never added to
 * [ScriptGeneration], [ScriptStudioDraft], logs, or export payloads.
 */
class GeminiScriptGenerator(
    private val apiKey: String,
    private val model: String = DEFAULT_MODEL,
    private val transport: GeminiScriptTransport = GeminiRestScriptTransport(),
) : ScriptGenerator {

    override suspend fun generate(request: ScriptRequest): Result<ScriptGeneration> {
        val validation = ScriptRequestValidator.validate(request)
        if (!validation.isValid) {
            return Result.failure(IllegalArgumentException(validation.errors.first()))
        }
        if (apiKey.isBlank()) {
            return Result.failure(IllegalStateException("Add a Gemini API key in Settings to create a live script."))
        }

        return transport.generate(
            model = model,
            apiKey = apiKey.trim(),
            prompt = ScriptStudioPrompt.compose(request),
        ).fold(
            onSuccess = { text ->
                val script = text.trim()
                if (script.isEmpty()) {
                    Result.failure(IllegalStateException("Gemini returned an empty script. Please try again."))
                } else {
                    Result.success(
                        ScriptGeneration(
                            title = "${request.mode.displayName()} script",
                            language = request.language.trim(),
                            mode = request.mode,
                            ttsText = script,
                            duration = ScriptDurationEstimator.estimate(script),
                            isDemo = false,
                        ),
                    )
                }
            },
            onFailure = { error ->
                Result.failure(error)
            },
        )
    }

    companion object {
        /** Gemini text model, deliberately separate from the app's Gemini Live audio model. */
        const val DEFAULT_MODEL = "gemini-3.7-flash"
    }
}

/** Builds a compact, safety-conscious prompt without exposing it to logs. */
internal object ScriptStudioPrompt {

    fun compose(request: ScriptRequest): String = buildString {
        appendLine("Create a realistic one-sided phone-call script for Kenza.")
        appendLine("Only output Kenza's audible side.")
        appendLine("Do not add speaker labels, listener dialogue, headings, explanations, or metadata.")
        appendLine("Represent listening time with occasional [listening pause N seconds] directions.")
        appendLine("Keep the relationship behavior warm, respectful, non-coercive, and non-manipulative.")
        appendLine("Do not claim to be a real person or invent private facts not supplied below.")
        appendLine()
        appendLine("Language: ${request.language.trim()}")
        appendLine("Mode: ${request.mode.displayName()}")
        appendLine("Requested duration: ${request.requestedMinutes} minutes")
        request.callReason?.takeIf { it.isNotBlank() }?.let { appendLine("Scenario: ${it.trim()}") }
        appendLine("Write natural spoken dialogue with varied line length and pauses.")
    }
}

/** The REST implementation follows Gemini's generateContent request format. */
class GeminiRestScriptTransport(
    private val http: OkHttpClient = OkHttpClient.Builder()
        .callTimeout(75, TimeUnit.SECONDS)
        .build(),
) : GeminiScriptTransport {

    override suspend fun generate(model: String, apiKey: String, prompt: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val payload = JSONObject().apply {
                    put(
                        "contents",
                        JSONArray().put(
                            JSONObject().put(
                                "parts",
                                JSONArray().put(JSONObject().put("text", prompt)),
                            ),
                        ),
                    )
                    put(
                        "generationConfig",
                        JSONObject()
                            .put("maxOutputTokens", MAX_OUTPUT_TOKENS)
                            .put("thinkingConfig", JSONObject().put("thinkingLevel", "low")),
                    )
                }
                val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/" +
                    "${model.trim()}:generateContent"
                val request = Request.Builder()
                    .url(endpoint)
                    .header("x-goog-api-key", apiKey)
                    .header("Content-Type", "application/json")
                    .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                    .build()

                http.newCall(request).execute().use { response ->
                    val body = response.body.string()
                    if (!response.isSuccessful) {
                        throw IllegalStateException("Gemini request failed (HTTP ${response.code}). Check Settings and try again.")
                    }
                    extractText(body).ifBlank {
                        throw IllegalStateException("Gemini returned no usable script. Please try again.")
                    }
                }
            }
        }

    private fun extractText(body: String): String {
        val parts = JSONObject(body)
            .optJSONArray("candidates")
            ?.optJSONObject(0)
            ?.optJSONObject("content")
            ?.optJSONArray("parts")
            ?: return ""
        return buildString {
            for (index in 0 until parts.length()) {
                append(parts.optJSONObject(index)?.optString("text").orEmpty())
            }
        }.trim()
    }

    private companion object {
        const val MAX_OUTPUT_TOKENS = 8_192
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}

private fun ScriptMode.displayName(): String = name
    .lowercase()
    .split('_')
    .joinToString(" ") { word -> word.replaceFirstChar(Char::uppercase) }
