package com.kenza.callsim.voice

import android.util.Base64
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kotlin.concurrent.thread

/** Google Gemini Live API provider. */
class GeminiLiveProvider(
    private val apiKey: String,
    private val model: String,
    private val voiceName: String,
    private val systemPrompt: String,
    private val listener: VoiceProvider.Listener,
    private val tokenBrokerUrl: String = "",
    private val adaptiveTurns: Boolean = false,
) : VoiceProvider {

    companion object {
        private const val TAG = "GeminiLive"
        private const val REALTIME_INPUT = "realtimeInput"
        private const val HOST = "generativelanguage.googleapis.com"
        private const val PUBLIC_METHOD = "google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent"
        private const val CONSTRAINED_METHOD = "google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContentConstrained"
        const val OUTPUT_SAMPLE_RATE = 24_000
        /** Consumer AI Studio Live native-audio model (affective dialog, low latency). */
        const val DEFAULT_MODEL = "gemini-2.5-flash-native-audio-preview-09-2025"
        const val DEFAULT_VOICE = "Kore"
    }

    private val http = OkHttpClient.Builder()
        .pingInterval(20, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()
    private val timing = LiveProviderTiming("gemini")
    private val lifecycleEpoch = AtomicLong(0L)

    private val connectionLock = Any()
    private val activityGate = AdaptiveActivityGate()
    @Volatile private var readyForAudio = false
    private var nextConnectionId = 0
    @Volatile private var activeConnectionId = 0
    @Volatile private var socket: WebSocket? = null
    @Volatile private var closedByUser = false
    @Volatile private var latestSessionHandle: String? = null
    @Volatile private var modelGenerating = false
    @Volatile private var callerSpeaking = false
    @Volatile private var checkpointCurrent = false
    @Volatile private var goAwayPending = false
    @Volatile private var sessionEphemeralToken: String? = null
    @Volatile private var consecutiveResumeAttempts = 0
    @Volatile private var continuityEnabled = true
    @Volatile private var retriedWithoutContinuity = false

    override fun start() {
        val epoch = lifecycleEpoch.incrementAndGet()
        timing.connectionStarted()
        closedByUser = false
        if (apiKey.isBlank() && tokenBrokerUrl.isBlank()) {
            listener.onClosed("No Gemini API key set. Open Settings and paste your key.", fatal = true)
            return
        }
        if (tokenBrokerUrl.isBlank()) {
            connect(resuming = false, lifecycle = epoch)
        } else thread(name = "gemini-token") {
            val token = fetchEphemeralToken()
            if (!isLifecycleActive(epoch)) return@thread
            if (token == null) {
                listener.onClosed("Ephemeral token broker request failed.", fatal = true)
            } else {
                sessionEphemeralToken = token
                connect(resuming = false, ephemeralToken = token, lifecycle = epoch)
            }
        }
    }

    private fun isLifecycleActive(epoch: Long): Boolean =
        !closedByUser && lifecycleEpoch.get() == epoch

    private fun connect(
        resuming: Boolean,
        ephemeralToken: String? = sessionEphemeralToken,
        lifecycle: Long = lifecycleEpoch.get(),
    ) {
        if (!isLifecycleActive(lifecycle)) return
        val connectionId = synchronized(connectionLock) {
            readyForAudio = false
            modelGenerating = false
            checkpointCurrent = false
            goAwayPending = false
            activityGate.reset()
            nextConnectionId += 1
            activeConnectionId = nextConnectionId
            activeConnectionId
        }
        val method = if (ephemeralToken == null) PUBLIC_METHOD else CONSTRAINED_METHOD
        val url = okhttp3.HttpUrl.Builder().scheme("https").host(HOST)
            .addPathSegments("ws/$method")
            .addQueryParameter(if (ephemeralToken == null) "key" else "access_token", ephemeralToken ?: apiKey.trim())
            .build().toString().replaceFirst("https://", "wss://")
        val request = Request.Builder().url(url).build()
        val webSocket = http.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                if (!isCurrent(connectionId) || !isLifecycleActive(lifecycle)) {
                    webSocket.close(1000, "stopped")
                    return
                }
                timing.socketConnected()
                Log.i(TAG, "open; sending setup resuming=$resuming continuity=$continuityEnabled")
                webSocket.send(buildSetup().toString())
                if (!resuming) listener.onConnected()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                if (isLifecycleActive(lifecycle)) handle(connectionId, text)
            }
            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                if (isLifecycleActive(lifecycle)) handle(connectionId, bytes.utf8())
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (!isCurrent(connectionId) || !isLifecycleActive(lifecycle)) return
                if (code == 1007 && resuming) {
                    timing.reconnectFailed()
                    listener.onClosed("Session checkpoint rejected; reconnecting with recent call context", fatal = false)
                    return
                }
                if (code == 1007 && !readyForAudio && continuityEnabled && !retriedWithoutContinuity) {
                    Log.w(TAG, "continuity setup rejected ($reason); retrying without it")
                    continuityEnabled = false
                    retriedWithoutContinuity = true
                    latestSessionHandle = null
                    replaceConnection(resuming = false, lifecycle = lifecycle, delayMs = 0)
                    return
                }
                val fatal = isFatal(code, reason)
                if (!fatal && resumeConnection(connectionId, lifecycle, planned = false)) return
                if (resuming) timing.reconnectFailed()
                listener.onClosed(
                    "closed ($code${if (reason.isNotBlank()) " $reason" else ""})",
                    fatal = fatal,
                )
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                if (!isCurrent(connectionId) || !isLifecycleActive(lifecycle)) return
                Log.e(TAG, "failure", t)
                val code = response?.code ?: 0
                val message = response?.let { "HTTP $code" } ?: (t.message ?: "connection failed")
                val fatal = code in 400..499
                if (!fatal && resumeConnection(connectionId, lifecycle, planned = false)) return
                if (resuming) timing.reconnectFailed()
                listener.onClosed(message, fatal = fatal)
            }
        })
        synchronized(connectionLock) {
            if (activeConnectionId == connectionId && isLifecycleActive(lifecycle)) {
                socket = webSocket
            } else {
                webSocket.cancel()
            }
        }
    }

    private fun fetchEphemeralToken(): String? = runCatching {
        require(tokenBrokerUrl.startsWith("https://"))
        http.newCall(Request.Builder().url(tokenBrokerUrl).get().build()).execute().use { response ->
            if (!response.isSuccessful) return@use null
            val json = JSONObject(response.body?.string().orEmpty())
            json.optString("token").ifBlank { json.optString("name") }.takeIf { it.isNotBlank() }
        }
    }.getOrNull()

    private fun buildSetup(): JSONObject {
        val speech = JSONObject().apply {
            put("voiceConfig", JSONObject().apply {
                put("prebuiltVoiceConfig", JSONObject().put("voiceName", voiceName.ifBlank { DEFAULT_VOICE }))
            })
        }
        val generationConfig = JSONObject().apply {
            put("responseModalities", JSONArray().put("AUDIO"))
            put("speechConfig", speech)
            put("temperature", 0.82)
            put("topP", 0.82)
        }
        val realtimeInputConfig = JSONObject().apply {
            put("automaticActivityDetection", JSONObject().apply {
                put("disabled", adaptiveTurns)
                if (!adaptiveTurns) {
                    put("startOfSpeechSensitivity", "START_SENSITIVITY_HIGH")
                    put("endOfSpeechSensitivity", "END_SENSITIVITY_HIGH")
                    put("prefixPaddingMs", GeminiLiveTuning.VAD_PREFIX_PADDING_MS)
                    put("silenceDurationMs", GeminiLiveTuning.VAD_SILENCE_DURATION_MS)
                }
            })
        }
        val setup = JSONObject().apply {
            put("model", "models/${model.ifBlank { DEFAULT_MODEL }}")
            put("generationConfig", generationConfig)
            put("realtimeInputConfig", realtimeInputConfig)
            if (continuityEnabled) {
                put("contextWindowCompression", JSONObject().put("slidingWindow", JSONObject()))
                put("sessionResumption", JSONObject().apply {
                    latestSessionHandle?.takeIf { it.isNotBlank() }?.let { put("handle", it) }
                })
            }
            if (systemPrompt.isNotBlank()) {
                put("systemInstruction", JSONObject().put(
                    "parts", JSONArray().put(JSONObject().put("text", systemPrompt))
                ))
            }
            put("inputAudioTranscription", JSONObject())
            put("outputAudioTranscription", JSONObject())
        }
        return JSONObject().put("setup", setup)
    }

    private fun handle(connectionId: Int, text: String) {
        if (!isCurrent(connectionId)) return
        val json = runCatching { JSONObject(text) }.getOrNull() ?: return
        json.optJSONObject("usageMetadata")?.let { usage ->
            listener.onUsage(
                usage.optInt("totalTokenCount").takeIf { usage.has("totalTokenCount") },
                usage.optInt("promptTokenCount").takeIf { usage.has("promptTokenCount") },
                usage.optInt("responseTokenCount").takeIf { usage.has("responseTokenCount") },
            )
        }
        when {
            json.has("setupComplete") -> {
                consecutiveResumeAttempts = 0
                goAwayPending = false
                synchronized(connectionLock) { readyForAudio = true }
                timing.sessionReady()
                timing.reconnectReady()
                listener.onReady(OUTPUT_SAMPLE_RATE)
            }
            json.has("sessionResumptionUpdate") -> {
                val update = json.optJSONObject("sessionResumptionUpdate") ?: return
                val handle = update.optString("newHandle").takeIf { it.isNotBlank() }
                checkpointCurrent = update.optBoolean("resumable") && handle != null
                if (checkpointCurrent) latestSessionHandle = handle
            }
            json.has("serverContent") -> {
                val content = json.getJSONObject("serverContent")
                if (content.optBoolean("interrupted")) {
                    modelGenerating = false
                    timing.interrupted()
                    listener.onInterrupted()
                }
                content.optJSONObject("inputTranscription")?.optString("text")
                    ?.takeIf { it.isNotEmpty() }?.let { finalized ->
                        if (adaptiveTurns) synchronized(connectionLock) { activityGate.onTranscript(finalized) }
                        timing.speechFinalized()
                        listener.onUserText(finalized)
                    }
                content.optJSONObject("outputTranscription")?.optString("text")
                    ?.takeIf { it.isNotEmpty() }?.let(listener::onAgentText)
                if (!content.optBoolean("interrupted")) content.optJSONObject("modelTurn")?.optJSONArray("parts")?.let { parts ->
                    modelGenerating = true
                    checkpointCurrent = false
                    for (index in 0 until parts.length()) {
                        val data = parts.optJSONObject(index)?.optJSONObject("inlineData")
                            ?.optString("data").orEmpty()
                        if (data.isNotEmpty()) {
                            timing.firstAudio()
                            runCatching { Base64.decode(data, Base64.DEFAULT) }
                                .getOrNull()?.let(listener::onAgentAudio)
                        }
                    }
                }
                if (content.optBoolean("generationComplete")) listener.onGenerationComplete()
                if (content.optBoolean("turnComplete")) listener.onTurnComplete()
                if (content.optBoolean("generationComplete") || content.optBoolean("turnComplete")) {
                    // Wait for turnComplete/checkpoint and caller silence, not simply generationComplete.
                    if (content.optBoolean("turnComplete")) modelGenerating = false
                }
            }
            json.has("goAway") -> {
                if (!goAwayPending) {
                    goAwayPending = true
                    timing.goAway()
                    scheduleGoAwayResume(connectionId, parseGoAwayDelayMs(json.optJSONObject("goAway")?.opt("timeLeft")))
                }
            }
        }
    }

    override fun sendAudio(pcm16le16k: ByteArray) {
        synchronized(connectionLock) {
            val currentSocket = socket
            if (!readyForAudio || currentSocket == null) {
                if (callerSpeaking) timing.droppedSpeechChunk()
                return
            }
            if (adaptiveTurns) sendAdaptiveAudio(currentSocket, pcm16le16k)
            else sendPcm(currentSocket, pcm16le16k)
        }
    }

    private fun sendAdaptiveAudio(ws: WebSocket, pcm: ByteArray) {
        val decision = activityGate.onAudio(pcm)
        if (decision.start) {
            callerSpeaking = true
            checkpointCurrent = false
            sendActivity(ws, "activityStart")
        }
        decision.packets.forEach { sendPcm(ws, it) }
        if (decision.end) {
            callerSpeaking = false
            sendActivity(ws, "activityEnd")
        }
    }

    private fun sendActivity(ws: WebSocket, name: String) {
        ws.send(JSONObject().put(REALTIME_INPUT, JSONObject().put(name, JSONObject())).toString())
    }

    private fun sendPcm(currentSocket: WebSocket, pcm16le16k: ByteArray) {
        val encoded = Base64.encodeToString(pcm16le16k, Base64.NO_WRAP)
        val message = JSONObject().put(
            REALTIME_INPUT,
            JSONObject().put(
                "audio",
                JSONObject()
                    .put("data", encoded)
                    .put("mimeType", "audio/pcm;rate=16000"),
            ),
        )
        currentSocket.send(message.toString())
        listener.onMicPacketQueued(currentSocket.queueSize())
    }

    override fun sendText(text: String) {
        if (text.isBlank()) return
        socket?.send(
            JSONObject()
                .put(REALTIME_INPUT, JSONObject().put("text", text))
                .toString()
        )
    }

    override fun endAudioStream() {
        synchronized(connectionLock) {
            val ws = socket ?: return
            if (!readyForAudio) return
            if (adaptiveTurns) {
                if (activityGate.finish()) sendActivity(ws, "activityEnd")
                callerSpeaking = false
            } else {
                ws.send(JSONObject().put(REALTIME_INPUT, JSONObject().put("audioStreamEnd", true)).toString())
            }
        }
    }

    override fun onUserSpeechActivity(active: Boolean) {
        if (!adaptiveTurns || !readyForAudio) callerSpeaking = active
        if (active) checkpointCurrent = false
    }

    private fun scheduleGoAwayResume(connectionId: Int, delayMs: Long) {
        val lifecycle = lifecycleEpoch.get()
        thread(name = "gemini-go-away") {
            val deadline = System.nanoTime() + delayMs * 1_000_000L
            while (isLifecycleActive(lifecycle) && isCurrent(connectionId) && goAwayPending) {
                val expired = System.nanoTime() >= deadline
                if (LiveReconnectPolicy.canRotate(latestSessionHandle != null, checkpointCurrent,
                        callerSpeaking, modelGenerating, expired)) {
                    resumeConnection(connectionId, lifecycle, planned = true)
                    break
                }
                if (expired) break  // No checkpoint: let closure trigger the normal recovery path.
                Thread.sleep(LiveReconnectPolicy.POLL_MS)
            }
        }
    }

    internal fun parseGoAwayDelayMs(value: Any?): Long = LiveReconnectPolicy.goAwayDelayMs(value)

    @Synchronized
    private fun resumeConnection(connectionId: Int, lifecycle: Long, planned: Boolean): Boolean {
        if (!isLifecycleActive(lifecycle) || !isCurrent(connectionId) || !continuityEnabled) return false
        if (latestSessionHandle.isNullOrBlank()) return false
        if (consecutiveResumeAttempts >= LiveReconnectPolicy.MAX_ATTEMPTS) return false
        consecutiveResumeAttempts += 1
        val atRisk = !checkpointCurrent || callerSpeaking || modelGenerating
        timing.reconnectStarted(atRisk)
        listener.onReconnecting(atRisk)
        replaceConnection(resuming = true, lifecycle = lifecycle,
            delayMs = LiveReconnectPolicy.retryDelayMs(consecutiveResumeAttempts, planned))
        return true
    }

    private fun replaceConnection(resuming: Boolean, lifecycle: Long, delayMs: Long) {
        if (!isLifecycleActive(lifecycle)) return
        val previous = synchronized(connectionLock) {
            readyForAudio = false
            activityGate.reset()
            activeConnectionId = ++nextConnectionId
            val old = socket
            socket = null
            old
        }
        runCatching { previous?.cancel() }
        thread(name = "gemini-reconnect") {
            if (delayMs > 0) Thread.sleep(delayMs)
            reconnectWithFreshAuthentication(resuming, lifecycle)
        }
    }

    private fun reconnectWithFreshAuthentication(resuming: Boolean, lifecycle: Long) {
        if (!isLifecycleActive(lifecycle)) return
        if (tokenBrokerUrl.isBlank()) {
            connect(resuming = resuming, lifecycle = lifecycle)
            return
        }
        // Broker tokens authorize new WebSocket connections only for a short time.
        // Retain the resumable handle, but fetch fresh authentication for every rotation.
        val refreshedToken = fetchEphemeralToken()
        if (!isLifecycleActive(lifecycle)) return
        if (refreshedToken.isNullOrBlank()) {
            if (resuming) timing.reconnectFailed()
            listener.onClosed("Ephemeral token refresh failed.", fatal = false)
            return
        }
        sessionEphemeralToken = refreshedToken
        connect(resuming = resuming, ephemeralToken = refreshedToken, lifecycle = lifecycle)
    }

    private fun isCurrent(connectionId: Int): Boolean = activeConnectionId == connectionId

    override fun timingSnapshot(): LiveProviderTimingSnapshot = timing.snapshot()

    override fun stop() {
        timing.teardownStarted()
        closedByUser = true
        lifecycleEpoch.incrementAndGet()
        val previous = synchronized(connectionLock) {
            readyForAudio = false
            latestSessionHandle = null
            sessionEphemeralToken = null
            activityGate.reset()
            activeConnectionId = ++nextConnectionId
            val old = socket
            socket = null
            old
        }
        runCatching { previous?.close(1000, "bye") }
        timing.teardownComplete()
    }

    private fun isFatal(code: Int, reason: String): Boolean {
        val lower = reason.lowercase()
        return code == 1008 ||
            lower.contains("api key") || lower.contains("permission") ||
            lower.contains("quota") || lower.contains("resource_exhausted") ||
            lower.contains("unauthenticated") || lower.contains("invalid")
    }
}
