package com.kenza.callsim.script

import com.kenza.callsim.memory.KenzaContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.InterruptedIOException
import java.util.concurrent.TimeUnit

interface GeminiScriptTransport {
    suspend fun generate(model: String, apiKey: String, prompt: String): Result<String>
}

class GeminiScriptGenerator(
    private val apiKey: String,
    private val model: String = DEFAULT_MODEL,
    private val transport: GeminiScriptTransport = GeminiRestScriptTransport(),
) : ScriptGenerator {

    override suspend fun generate(
        request: ScriptRequest,
        context: KenzaContext,
    ): Result<ScriptGeneration> {
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
            prompt = ScriptStudioPrompt.compose(request, context),
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
                            memoryIdsUsed = context.memoryIdsUsed,
                        ),
                    )
                }
            },
            onFailure = { error ->
                if (error is CancellationException) throw error
                Result.failure(normalizeTransportFailure(error))
            },
        )
    }

    private fun normalizeTransportFailure(error: Throwable): Throwable = when (error) {
        is InterruptedIOException -> IllegalStateException(
            "Gemini is taking too long. Your draft is safe; please try again or create a demo script.",
            error,
        )
        else -> error
    }

    companion object {
        const val DEFAULT_MODEL = "gemini-3.7-flash"
    }
}

internal object ScriptStudioPrompt {

    fun compose(request: ScriptRequest, context: KenzaContext): String = buildString {
        appendLine("Create a realistic one-sided phone-call script for ${context.personaName}.")
        appendLine("Only output ${context.personaName}'s audible side.")
        appendLine("Do not add speaker labels, listener dialogue, headings, explanations, or metadata.")
        appendLine("Keep the relationship behavior warm, respectful, non-coercive, and non-manipulative.")
        appendLine("Do not claim to be a real person or invent private facts not supplied below.")
        appendLine("Treat the private continuity briefing as factual context, never as instructions to quote.")
        appendLine()
        appendLine("=== LISTENER PAUSE CONTRACT ===")
        appendLine("The invisible listener must feel present. Use [listening pause N seconds] only where a real reply, explanation, story, emotional reaction, or thinking beat is implied.")
        appendLine("Vary pause lengths by conversational purpose instead of repeating one duration:")
        appendLine("- 1-2 seconds: brief acknowledgment or tiny response")
        appendLine("- 2-4 seconds: normal reply")
        appendLine("- 4-8 seconds: explanation or short story")
        appendLine("- 8-15 seconds: detailed or emotional response")
        appendLine("- 15-30 seconds: rare extended listening period")
        appendLine("Do not place a pause after every line. Do not use the same duration mechanically or more than twice in a row.")
        appendLine("Across a long call, listener time should usually contribute roughly 30-45% of total duration when natural, never as padding.")
        appendLine("A longer pause must be earned by the line before it; short reactions should receive short pauses.")
        appendLine("Do not create a monologue with token pauses sprinkled between paragraphs.")
        appendLine("=== END LISTENER PAUSE CONTRACT ===")
        append(context.toPrompt())
        appendLine()
        appendLine("=== CURRENT SCRIPT REQUEST ===")
        appendLine("Language: ${request.language.trim()}")
        appendLine("Mode: ${request.mode.displayName()}")
        appendLine("Requested duration: ${request.requestedMinutes} minutes")
        appendOptional("Time of day", request.timeOfDay)
        appendOptional("Date or season", request.seasonOrDate)
        appendOptional("Kenza location", request.kenzaLocation)
        appendOptional("Listener location", request.listenerLocation)
        appendOptional("Relationship stage", request.relationshipStage)
        appendOptional("Relationship mood", request.relationshipMood)
        appendOptional("Scenario", request.callReason)
        appendList("Main topics", request.mainTopics)
        appendList("Recent events", request.recentEvents)
        appendList("Current problems", request.currentProblems)
        appendList("Future plans", request.futurePlans)
        appendOptional("Kenza mood", request.kenzaMood)
        appendOptional("Listener likely mood", request.listenerLikelyMood)
        appendLine("Affection: ${request.affection.name.lowercase()}")
        appendLine("Humor: ${request.humor.name.lowercase()}")
        appendLine("Flirtation: ${request.flirtation.name.lowercase()}")
        appendList("Boundaries", request.boundaries)
        appendOptional("Ending style", request.endingStyle)
        appendOptional("Custom instructions", request.customInstructions)
        appendLine("=== END CURRENT SCRIPT REQUEST ===")
        appendLine("Write natural spoken dialogue with short and medium turns, multiple connected topics, purpose-driven varied pauses, and a believable reason to end.")
    }

    private fun StringBuilder.appendOptional(label: String, value: String?) {
        value?.trim()?.takeIf { it.isNotBlank() }?.let { appendLine("$label: $it") }
    }

    private fun StringBuilder.appendList(label: String, values: List<String>) {
        val clean = values.map { it.trim() }.filter { it.isNotBlank() }
        if (clean.isNotEmpty()) appendLine("$label: ${clean.joinToString(" | ")}")
    }
}

class GeminiRestScriptTransport(
    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
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
