package com.kenza.callsim.voice

import org.json.JSONObject

/**
 * Server events of the ElevenLabs Agents WebSocket that the app acts on, parsed without touching
 * Android APIs so the protocol logic is JVM-testable. Field names follow the official AsyncAPI
 * spec (`/v1/convai/conversation`).
 */
internal sealed class ElevenLabsServerEvent {
    data class Metadata(val outputFormat: String, val inputFormat: String) : ElevenLabsServerEvent()
    data class Audio(val base64: String, val eventId: Int?, val isFinal: Boolean) : ElevenLabsServerEvent()
    data class UserTranscript(val text: String) : ElevenLabsServerEvent()
    data class AgentResponse(val text: String) : ElevenLabsServerEvent()
    data class AgentResponseCorrection(val original: String, val corrected: String) : ElevenLabsServerEvent()
    data class AgentResponseComplete(val eventId: Int?) : ElevenLabsServerEvent()
    data class Interruption(val eventId: Int?) : ElevenLabsServerEvent()
    data class Ping(val eventId: Int) : ElevenLabsServerEvent()
    data class Other(val type: String) : ElevenLabsServerEvent()

    companion object {
        fun parse(json: JSONObject): ElevenLabsServerEvent {
            val type = json.optString("type")
            return when (type) {
                "conversation_initiation_metadata" ->
                    json.optJSONObject("conversation_initiation_metadata_event").let {
                        Metadata(it.string("agent_output_audio_format"), it.string("user_input_audio_format"))
                    }
                "audio" -> json.optJSONObject("audio_event").let {
                    val isFinal = it?.optBoolean("is_final", false) ?: false
                    Audio(it.string("audio_base_64"), it.intOrNull(EVENT_ID), isFinal)
                }
                "user_transcript" ->
                    UserTranscript(json.optJSONObject("user_transcription_event").string("user_transcript"))
                "agent_response" -> AgentResponse(json.optJSONObject("agent_response_event").string("agent_response"))
                "agent_response_correction" -> json.optJSONObject("agent_response_correction_event").let {
                    AgentResponseCorrection(it.string("original_agent_response"), it.string("corrected_agent_response"))
                }
                "agent_response_complete" ->
                    AgentResponseComplete(json.optJSONObject("agent_response_complete_event").intOrNull(EVENT_ID))
                "interruption" -> Interruption(json.optJSONObject("interruption_event").intOrNull(EVENT_ID))
                "ping" -> json.optJSONObject("ping_event").intOrNull(EVENT_ID)?.let(::Ping) ?: Other(type)
                else -> Other(type)
            }
        }

        private const val EVENT_ID = "event_id"

        private fun JSONObject?.string(name: String): String = this?.optString(name).orEmpty()

        private fun JSONObject?.intOrNull(name: String): Int? =
            if (this != null && has(name) && !isNull(name)) optInt(name) else null
    }
}

/**
 * Drops agent audio that belongs to a response the user already interrupted. Mirrors the official
 * ElevenLabs JS client: a chunk plays only when its `event_id` is not older than the latest
 * `interruption_event.event_id`. Without this, late chunks of a cut-off reply can still play.
 */
internal class InterruptedAudioFilter {
    @Volatile private var lastInterruptionEventId = Int.MIN_VALUE

    fun onInterruption(eventId: Int?) {
        if (eventId != null && eventId > lastInterruptionEventId) lastInterruptionEventId = eventId
    }

    /** Chunks without an id cannot be classified, so they are kept rather than silently lost. */
    fun shouldPlay(audioEventId: Int?): Boolean = audioEventId == null || audioEventId >= lastInterruptionEventId

    fun reset() {
        lastInterruptionEventId = Int.MIN_VALUE
    }
}

/** Client-to-server messages. */
internal object ElevenLabsClientMessages {
    /** The microphone stream format this app always sends. */
    const val EXPECTED_INPUT_FORMAT = "pcm_16000"

    /**
     * `conversation_initiation_client_data`. Dynamic variables need no Security override; the full
     * system-prompt override is only included when the user explicitly enabled it.
     */
    fun initiation(dynamicVariables: Map<String, String>, promptOverride: String?): JSONObject {
        val init = JSONObject().put("type", "conversation_initiation_client_data")
        if (dynamicVariables.isNotEmpty()) {
            val vars = JSONObject()
            dynamicVariables.forEach { (key, value) -> vars.put(key, value) }
            init.put("dynamic_variables", vars)
        }
        if (!promptOverride.isNullOrBlank()) {
            val agent = JSONObject().put("prompt", JSONObject().put("prompt", promptOverride))
            init.put("conversation_config_override", JSONObject().put("agent", agent))
        }
        return init
    }

    /** Non-interrupting background context; never appears to the LLM as something the user said. */
    fun contextualUpdate(text: String, contextId: String? = null): JSONObject =
        JSONObject().put("type", "contextual_update").put("text", text).apply {
            contextId?.takeIf { it.isNotBlank() }?.let { put("context_id", it) }
        }

    /** A real user turn. Only used when a reply must be triggered (the speak-first fallback). */
    fun userMessage(text: String): JSONObject = JSONObject().put("type", "user_message").put("text", text)

    fun pong(eventId: Int): JSONObject = JSONObject().put("type", "pong").put("event_id", eventId)

    fun sampleRateOf(format: String): Int = format.substringAfterLast('_').toIntOrNull() ?: 16_000
}
