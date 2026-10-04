package com.kenza.callsim.voice

import android.util.Base64
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/** ElevenLabs Conversational AI provider (premium option, Kenza's cloned voice). */
class ElevenLabsProvider(
    private val agentId: String,
    private val apiKey: String,
    private val listener: VoiceProvider.Listener,
    private val promptOverride: String? = null,
    /** Sent as `dynamic_variables`; the agent prompt/first message reference them as {{name}}. */
    private val dynamicVariables: Map<String, String> = emptyMap(),
) : VoiceProvider {

    companion object {
        private const val TAG = "ElevenLabsProvider"
        private const val BASE = "wss://api.elevenlabs.io/v1/convai/conversation"
        private const val SIGNED_URL =
            "https://api.elevenlabs.io/v1/convai/conversation/get-signed-url"
    }

    private val http = OkHttpClient.Builder()
        .pingInterval(20, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()
    private val timing = LiveProviderTiming("elevenlabs")
    private val lifecycleEpoch = AtomicLong(0L)
    private val audioFilter = InterruptedAudioFilter()

    @Volatile private var socket: WebSocket? = null
    @Volatile private var closedByUser = false
    @Volatile private var outputIsMulaw = false
    @Volatile private var sessionReady = false
    @Volatile private var publicFallbackHint: String? = null

    override fun start() {
        val epoch = lifecycleEpoch.incrementAndGet()
        timing.connectionStarted()
        closedByUser = false
        sessionReady = false
        publicFallbackHint = null
        audioFilter.reset()

        val aid = agentId.trim()
        val key = apiKey.trim()
        if (aid.isEmpty()) {
            listener.onClosed("No ElevenLabs Agent ID set. Open Settings to add one.", fatal = true)
            return
        }

        Thread {
            val url = try {
                when (ElevenLabsAuth.classifyApiKey(key)) {
                    ElevenLabsAuth.ApiKeyKind.EMPTY -> publicUrl(aid)
                    ElevenLabsAuth.ApiKeyKind.KEY_ID_OR_INVALID -> {
                        publicFallbackHint = ElevenLabsAuth.SECRET_KEY_HINT
                        publicUrl(aid)
                    }
                    ElevenLabsAuth.ApiKeyKind.SECRET -> {
                        try {
                            fetchSignedUrl(aid, key)
                        } catch (e: InvalidElevenLabsApiKeyException) {
                            Log.w(TAG, "ElevenLabs API key rejected; trying public agent connection")
                            publicFallbackHint = ElevenLabsAuth.SECRET_KEY_HINT
                            publicUrl(aid)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "signed url failed", e)
                if (isLifecycleActive(epoch)) {
                    listener.onClosed(sanitizeStartFailure(e), fatal = true)
                }
                return@Thread
            }
            // A signed-URL request may outlive a hang-up. Never resurrect a stopped session.
            if (!isLifecycleActive(epoch)) return@Thread
            connect(url, epoch)
        }.start()
    }

    private fun isLifecycleActive(epoch: Long): Boolean =
        !closedByUser && lifecycleEpoch.get() == epoch

    private fun publicUrl(aid: String): String = "$BASE?agent_id=$aid"

    private fun fetchSignedUrl(aid: String, key: String): String {
        val req = Request.Builder()
            .url("$SIGNED_URL?agent_id=$aid")
            .header("xi-api-key", key)
            .get()
            .build()
        http.newCall(req).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                if (ElevenLabsAuth.isInvalidApiKeyFailure(body)) {
                    throw InvalidElevenLabsApiKeyException()
                }
                val detail = extractApiError(body)
                val suffix = if (detail.isBlank()) "" else ": $detail"
                error("ElevenLabs signed URL request failed (HTTP ${resp.code})$suffix")
            }
            return JSONObject(body).getString("signed_url")
        }
    }

    private fun connect(url: String, epoch: Long) {
        if (!isLifecycleActive(epoch)) return
        val req = Request.Builder().url(url).build()
        val webSocket = http.newWebSocket(req, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                if (!isLifecycleActive(epoch)) {
                    webSocket.close(1000, "stopped")
                    return
                }
                timing.socketConnected()
                Log.i(TAG, "socket open")
                webSocket.send(buildInitMessage())
                listener.onConnected()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                if (isLifecycleActive(epoch)) handle(webSocket, text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.i(TAG, "socket closed $code $reason")
                if (isLifecycleActive(epoch)) {
                    val fallback = publicFallbackHint
                    val resolvedReason = if (
                        !sessionReady && fallback != null &&
                        ElevenLabsAuth.isLikelyAuthorizationFailure(code, reason)
                    ) fallback else "closed ($code${if (reason.isNotBlank()) " $reason" else ""})"
                    listener.onClosed(
                        resolvedReason,
                        fatal = if (resolvedReason === fallback) true else isFatal(code, reason)
                    )
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "socket failure", t)
                if (!isLifecycleActive(epoch)) return
                val code = response?.code ?: 0
                val fallback = publicFallbackHint
                val rawDetail = response?.let { "HTTP $code" } ?: t.message ?: "connection failed"
                val detail = if (
                    !sessionReady && fallback != null &&
                    ElevenLabsAuth.isLikelyAuthorizationFailure(code, rawDetail)
                ) fallback else rawDetail
                listener.onClosed(detail, fatal = code in 400..499 || detail === fallback)
            }
        })
        if (isLifecycleActive(epoch)) socket = webSocket else webSocket.cancel()
    }

    private fun buildInitMessage(): String =
        ElevenLabsClientMessages.initiation(dynamicVariables, promptOverride).toString()

    private fun handle(webSocket: WebSocket, text: String) {
        val json = runCatching { JSONObject(text) }.getOrNull() ?: return
        when (val event = ElevenLabsServerEvent.parse(json)) {
            is ElevenLabsServerEvent.Metadata -> {
                sessionReady = true
                timing.sessionReady()
                publicFallbackHint = null
                val fmt = event.outputFormat
                outputIsMulaw = fmt.startsWith("ulaw", ignoreCase = true)
                val sampleRate = ElevenLabsClientMessages.sampleRateOf(fmt)
                Log.i(TAG, "agent output format=$fmt -> rate=$sampleRate mulaw=$outputIsMulaw")
                if (event.inputFormat.isNotBlank() &&
                    event.inputFormat != ElevenLabsClientMessages.EXPECTED_INPUT_FORMAT
                ) {
                    // The mic always streams 16 kHz PCM; a different agent input format degrades STT.
                    Log.w(TAG, "agent expects user_input_audio_format=${event.inputFormat}; app sends pcm_16000")
                }
                listener.onReady(sampleRate)
            }
            is ElevenLabsServerEvent.Audio -> {
                // Late chunks from a reply the user already interrupted must never play.
                if (event.base64.isEmpty() || !audioFilter.shouldPlay(event.eventId)) return
                timing.firstAudio()
                runCatching { Base64.decode(event.base64, Base64.DEFAULT) }.getOrNull()?.let { raw ->
                    val pcm = if (outputIsMulaw) ulawToPcm16(raw) else raw
                    listener.onAgentAudio(pcm)
                }
                if (event.isFinal) listener.onGenerationComplete()
            }
            is ElevenLabsServerEvent.UserTranscript -> if (event.text.isNotEmpty()) {
                timing.speechFinalized()
                listener.onUserText(event.text)
            }
            is ElevenLabsServerEvent.AgentResponse ->
                if (event.text.isNotEmpty()) listener.onAgentText(event.text)
            is ElevenLabsServerEvent.AgentResponseCorrection ->
                if (event.corrected != event.original) {
                    listener.onAgentTextCorrected(event.original, event.corrected)
                }
            // Requires agent_response_complete in the agent's client_events (see agent_configs/).
            is ElevenLabsServerEvent.AgentResponseComplete -> listener.onTurnComplete()
            is ElevenLabsServerEvent.Interruption -> {
                audioFilter.onInterruption(event.eventId)
                timing.interrupted()
                listener.onInterrupted()
            }
            is ElevenLabsServerEvent.Ping ->
                webSocket.send(ElevenLabsClientMessages.pong(event.eventId).toString())
            is ElevenLabsServerEvent.Other -> Unit
        }
    }

    override fun sendAudio(pcm16le16k: ByteArray) {
        val s = socket ?: return
        val b64 = Base64.encodeToString(pcm16le16k, Base64.NO_WRAP)
        s.send("""{"user_audio_chunk":"$b64"}""")
        listener.onMicPacketQueued(s.queueSize())
    }

    /** A real user turn that triggers a reply. Reserved for the speak-first fallback. */
    override fun sendText(text: String) {
        if (text.isBlank()) return
        socket?.send(ElevenLabsClientMessages.userMessage(text).toString())
    }

    /** Private director cues go out as non-interrupting `contextual_update`, not as user speech. */
    override fun sendContext(text: String) {
        if (text.isBlank()) return
        socket?.send(ElevenLabsClientMessages.contextualUpdate(text).toString())
    }

    override fun timingSnapshot(): LiveProviderTimingSnapshot = timing.snapshot()

    override fun stop() {
        timing.teardownStarted()
        closedByUser = true
        lifecycleEpoch.incrementAndGet()
        val previous = socket
        socket = null
        runCatching { previous?.close(1000, "bye") }
        timing.teardownComplete()
    }

    private fun isFatal(code: Int, reason: String): Boolean {
        val r = reason.lowercase()
        return code == 1008 || r.contains("quota") || r.contains("credit") ||
            r.contains("unauthorized") || r.contains("limit") || r.contains("exceeded") ||
            r.contains("authentication") || r.contains("authorization") ||
            // A rejected dynamic-variable set fails identically on every retry.
            r.contains("dynamic variable")
    }

    private fun sanitizeStartFailure(e: Exception): String = when (e) {
        is InvalidElevenLabsApiKeyException -> ElevenLabsAuth.SECRET_KEY_HINT
        else -> e.message?.takeIf { it.isNotBlank() } ?: "Could not start ElevenLabs session."
    }

    private fun extractApiError(body: String): String {
        val root = runCatching { JSONObject(body) }.getOrNull() ?: return ""
        val detail = root.optJSONObject("detail")
        return detail?.optString("message")?.takeIf { it.isNotBlank() }
            ?: root.optString("message").takeIf { it.isNotBlank() }
            ?: ""
    }

    private fun ulawToPcm16(ulaw: ByteArray): ByteArray {
        val bias = 0x84
        val out = ByteArray(ulaw.size * 2)
        for (i in ulaw.indices) {
            val u = ulaw[i].toInt().inv() and 0xFF
            val exponent = (u shr 4) and 0x07
            val mantissa = u and 0x0F
            var sample = ((mantissa shl 3) + bias) shl exponent
            sample -= bias
            if (u and 0x80 != 0) sample = -sample
            out[i * 2] = (sample and 0xFF).toByte()
            out[i * 2 + 1] = ((sample shr 8) and 0xFF).toByte()
        }
        return out
    }

    private class InvalidElevenLabsApiKeyException : Exception(ElevenLabsAuth.SECRET_KEY_HINT)
}
