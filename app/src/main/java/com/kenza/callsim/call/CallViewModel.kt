package com.kenza.callsim.call

import android.app.Application
import android.content.Context
import android.media.AudioManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kenza.callsim.config.ConfigRepository
import com.kenza.callsim.config.ProviderType
import com.kenza.callsim.config.SettingsData
import com.kenza.callsim.initiative.InitiativeEngine
import com.kenza.callsim.initiative.InitiativeStore
import com.kenza.callsim.memory.MemoryContext
import com.kenza.callsim.memory.MemoryExtractor
import com.kenza.callsim.memory.MemoryStore
import com.kenza.callsim.schedule.DeferredScheduledCall
import com.kenza.callsim.schedule.IncomingCallService
import com.kenza.callsim.schedule.ScheduledCallRuntime
import com.kenza.callsim.voice.ElevenLabsProvider
import com.kenza.callsim.voice.GeminiLiveProvider
import com.kenza.callsim.voice.LiveTurnTelemetry
import com.kenza.callsim.voice.LocalSpeechEnergyTracker
import com.kenza.callsim.voice.MicRecorder
import com.kenza.callsim.voice.PcmPlayer
import com.kenza.callsim.voice.TranscriptAssembler
import com.kenza.callsim.voice.VoiceProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CallViewModel(app: Application) : AndroidViewModel(app) {

    private val config = ConfigRepository(app)

    private val _state = MutableStateFlow(
        CallUiState(
            contactName = config.contactName,
            isConfigured = config.isConfigured
        )
    )
    val state: StateFlow<CallUiState> = _state.asStateFlow()

    private val audioManager =
        app.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val ringtone = RingtonePlayer(app)
    private val dtmf = DtmfTones()

    private var client: VoiceProvider? = null
    private var mic: MicRecorder? = null
    private var player: PcmPlayer? = null
    private var telemetry = LiveTurnTelemetry(false)
    private val speechEnergy = LocalSpeechEnergyTracker(
        endHangoverChunks = com.kenza.callsim.voice.GeminiLiveTuning.CLIENT_ENDPOINT_HANGOVER_CHUNKS,
    )
    private var lastClientEndpointMs = 0L
    private val transcriptAssembler = TranscriptAssembler()
    private val conversationRepair = ConversationRepairEngine()

    // Long-term memory: transcript accumulates during a call and is distilled
    // into durable memories when it ends.
    private val memory = MemoryStore(app)
    private val initiativeStore = InitiativeStore(app)
    private val transcript = mutableListOf<Pair<String, String>>()
    private var callStartedAt = 0L

    // When Kenza initiated the call, this opaque token points back into the
    // encrypted memory snapshot. No memory text is copied into Android prefs.
    private var pendingInitiativeToken: String? = null
    private var initiativeOpeningSent = false

    // Opening/reconnect state. Once anything was heard in this call, a reconnect or account
    // failover must not replay Kenza's greeting. Session flags reset with each provider session.
    private var openingHeardThisCall = false
    private var sessionAgentAudioHeard = false
    private var sessionOpeningLine = ""
    private var sessionProviderName = ""

    // Natural call ending and one-shot conversational repair timing.
    private var endCallJob: Job? = null
    private var silenceRepairJob: Job? = null

    private var timerJob: Job? = null
    private var speakingResetJob: Job? = null
    private var dialingJob: Job? = null
    private var pendingStartAfterPermission = false

    // Auto-reconnect: when a provider ends a session (e.g. a server-side
    // conversation-duration limit) we transparently re-establish the call. To
    // avoid replay loops (e.g. ElevenLabs out of quota replaying its greeting),
    // the retry budget only refreshes once the user has actually spoken — proof
    // of a real two-way call rather than a failing reconnect cycle. Fatal causes
    // (out of quota, bad key, mic failure) never reconnect.
    private var userEnded = false
    private var reconnectAttempts = 0
    private val maxConsecutiveReconnects = 4
    private var hadUserInteraction = false

    // ElevenLabs multi-key failover: rotate through (agentId, apiKey) pairs as
    // each account runs out of credit, so the call keeps going uninterrupted.
    private var elevenCreds: List<Pair<String, String>> = emptyList()
    private var credIndex = 0

    // ---- Incoming / outgoing flow -------------------------------------------

    fun simulateIncomingCall() {
        if (!CallTransitionRules.canPlaceCall(_state.value.phase)) return
        if (!ScheduledCallRuntime.tryStartUserInitiatedCall()) return
        armIncomingInitiative(explicitToken = null, countAgainstCooldown = false)
        _state.update { it.copy(phase = CallPhase.INCOMING, errorMessage = null) }
        ringtone.start()
    }

    /**
     * Triggered by a fired schedule (via the full-screen notification). Rings
     * from any idle/ended state; ignored if a call is already in progress.
     *
     * Autonomous calls arrive with a pre-selected memory-backed token. Explicit
     * user schedules do not, so we select a fresh topic at ring time and still
     * let Kenza own the opening instead of making the user host her call.
     */
    fun onScheduledIncomingCall(initiativeToken: String? = null) {
        val phase = _state.value.phase
        if (phase == CallPhase.INCOMING) return // duplicate full-screen notification intent
        if (phase != CallPhase.IDLE && phase != CallPhase.ENDED) {
            // A last-moment UI race must never leave the foreground service ringing.
            IncomingCallService.stop(getApplication())
            return
        }
        armIncomingInitiative(initiativeToken, countAgainstCooldown = initiativeToken == null)
        // The foreground IncomingCallService already rings + vibrates; just show
        // the incoming UI so we don't double-ring.
        _state.update { it.copy(phase = CallPhase.INCOMING, errorMessage = null) }
    }

    private fun armIncomingInitiative(explicitToken: String?, countAgainstCooldown: Boolean) {
        val now = System.currentTimeMillis()
        val selected = explicitToken ?: InitiativeEngine.bestSeed(
            snapshot = memory.snapshot(),
            now = now,
            excludedTokens = initiativeStore.history().recentTokens,
        )?.token ?: InitiativeEngine.SOCIAL_TOKEN

        pendingInitiativeToken = selected
        initiativeOpeningSent = false
        if (countAgainstCooldown) initiativeStore.markInitiated(selected, now)
    }

    fun placeCall() {
        if (!CallTransitionRules.canPlaceCall(_state.value.phase)) return
        if (!ScheduledCallRuntime.tryStartUserInitiatedCall()) return
        // Mohamed initiated this call. Kenza still has conversational agency once
        // it starts, but she does not pretend that she was the caller.
        pendingInitiativeToken = null
        initiativeOpeningSent = false
        ringtone.stop()
        _state.update { it.copy(phase = CallPhase.DIALING, errorMessage = null) }
        dialingJob?.cancel()
        // Start provider while "calling…" is still shown so handshake latency is hidden.
        dialingJob = viewModelScope.launch {
            beginConnecting()
        }
    }

    fun answerIncoming() {
        if (!CallTransitionRules.canAnswerIncoming(_state.value.phase)) return
        ringtone.stop()
        IncomingCallService.stop(getApplication())
        beginConnecting()
    }

    fun declineIncoming() {
        if (!CallTransitionRules.canAnswerIncoming(_state.value.phase)) return
        ringtone.stop()
        IncomingCallService.stop(getApplication())
        endCall()
    }

    private fun beginConnecting() {
        userEnded = false
        reconnectAttempts = 0
        hadUserInteraction = false
        initiativeOpeningSent = false
        openingHeardThisCall = false
        elevenCreds = config.elevenCredentials()
        credIndex = 0
        transcript.clear()
        callStartedAt = System.currentTimeMillis()
        _state.update { it.copy(phase = CallPhase.CONNECTING) }
        if (!config.isConfigured) {
            // Pure UI demo — show the live call screen without needing the mic.
            onSessionActive(demo = true)
        } else {
            ensureMicThenStart()
        }
    }

    // ---- Mic permission ------------------------------------------------------

    private fun ensureMicThenStart() {
        val ctx = getApplication<Application>()
        val granted = ctx.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        if (granted) {
            startVoiceSession()
        } else {
            pendingStartAfterPermission = true
            // UI observes this and launches the system permission dialog.
            _state.update { it.copy(errorMessage = NEED_MIC) }
        }
    }

    fun onMicPermissionResult(granted: Boolean) {
        if (_state.value.errorMessage == NEED_MIC) {
            _state.update { it.copy(errorMessage = null) }
        }
        if (!pendingStartAfterPermission) return
        pendingStartAfterPermission = false
        if (granted) {
            startVoiceSession()
        } else {
            finishCall("Microphone permission is required for the call.")
        }
    }

    // ---- Live voice session --------------------------------------------------

    private fun startVoiceSession() {
        if (!config.isConfigured) {
            // Still allow the UI to "connect" in demo mode so the call screen works.
            onSessionActive(demo = true)
            return
        }

        configureAudioRouting()
        telemetry = LiveTurnTelemetry(config.diagnosticsEnabled)
        speechEnergy.reset()
        sessionAgentAudioHeard = false
        sessionOpeningLine = ""
        sessionProviderName = config.provider.name.lowercase()

        val ai = buildProvider(object : VoiceProvider.Listener {
            override fun onConnected() {
                _state.update { it.copy(phase = CallPhase.CONNECTING) }
            }
            override fun onReady(outputSampleRate: Int) {
                // Now we know the agent's audio rate — open the player to match,
                // then start streaming the mic and go live.
                if (player == null) {
                    player = PcmPlayer(outputSampleRate, telemetry.playback).also { it.start() }
                }
                if (mic == null) {
                    speechEnergy.reset()
                    mic = MicRecorder(
                        onChunk = { chunk ->
                            when (speechEnergy.onPcm(chunk)) {
                                LocalSpeechEnergyTracker.Event.START ->
                                    telemetry.localSpeechStart()
                                LocalSpeechEnergyTracker.Event.END -> {
                                    telemetry.localSpeechEnd(speechEnergy.lastVoicedMs() ?: System.currentTimeMillis())
                                    maybeClientEndpoint()
                                }
                                null -> Unit
                            }
                            client?.sendAudio(chunk)
                        },
                        onError = { msg -> handleDisconnect("Microphone: $msg", fatal = true) },
                        onStreaming = { _state.update { it.copy(micStreaming = true) } }
                    ).also {
                        it.speakerphoneOn = _state.value.isSpeakerOn
                        it.start()
                    }
                }
                onSessionActive(demo = false)
            }
            override fun onAgentAudio(pcm: ByteArray) {
                sessionAgentAudioHeard = true
                openingHeardThisCall = true
                mic?.agentSpeaking = true
                telemetry.modelAudio()
                player?.enqueue(pcm)
                markSpeaking()
            }
            override fun onUserText(text: String) {
                // Real two-way interaction — a genuine call, so allow further reconnects.
                hadUserInteraction = true
                openingHeardThisCall = true
                player?.markTurnBoundary()
                silenceRepairJob?.cancel(); silenceRepairJob = null
                transcriptAssembler.appendUser(text)
                val repair = conversationRepair.onUserText(text)
                onConversationLine(fromUser = true, text = text, repair = repair)
                repair?.let(::dispatchConversationRepair)
                _state.update { it.copy(lastUserText = text, activity = AgentActivity.THINKING) }
            }
            override fun onAgentText(text: String) {
                conversationRepair.onAgentText(text)
                transcriptAssembler.appendAgent(text)
                onConversationLine(fromUser = false, text = text)
                _state.update { it.copy(lastAgentText = text) }
            }
            override fun onAgentTextCorrected(original: String, corrected: String) {
                // ElevenLabs truncates the agent's text to what was actually spoken before an
                // interruption; keep transcript and memory faithful to what Mohamed heard.
                if (!transcriptAssembler.correctAgent(original, corrected)) {
                    TranscriptAssembler.correctCommitted(transcript, original, corrected)
                }
                _state.update {
                    if (it.lastAgentText.trim() == original.trim()) it.copy(lastAgentText = corrected) else it
                }
            }
            override fun onInterrupted() {
                if (speechEnergy.forceEnd() == LocalSpeechEnergyTracker.Event.END) {
                    telemetry.localSpeechEnd(speechEnergy.lastVoicedMs() ?: System.currentTimeMillis())
                }
                telemetry.interrupted()
                silenceRepairJob?.cancel(); silenceRepairJob = null
                transcript += transcriptAssembler.commit()
                // Flush uses PLAYBACK_WRITE_CHUNK_MS-sized queue epochs so barge-in
                // discards unsounded audio without waiting on a blocking write.
                player?.flush()
                _state.update { it.copy(activity = AgentActivity.LISTENING) }
            }
            override fun onUsage(total: Int?, prompt: Int?, response: Int?) = telemetry.usage(total)
            override fun onMicPacketQueued(socketQueueBytes: Long) = telemetry.micQueued(socketQueueBytes)
            override fun onGenerationComplete() = telemetry.generationComplete()
            override fun onTurnComplete() {
                player?.markTurnBoundary()
                transcript += transcriptAssembler.commit()
                conversationRepair.onAgentTurnComplete(System.currentTimeMillis())
                armSilenceRepairCheck()
                telemetry.complete(
                    route = if (_state.value.isSpeakerOn) "speakerphone" else "earpiece_or_headset",
                    sessionAgeMs = (System.currentTimeMillis() - callStartedAt).coerceAtLeast(0),
                )
            }
            override fun onClosed(reason: String, fatal: Boolean) =
                handleDisconnect("Disconnected: $reason", fatal)
        })
        client = ai
        ai.start()
    }

    private fun buildProvider(listener: VoiceProvider.Listener): VoiceProvider {
        val now = System.currentTimeMillis()
        val memoryContext = MemoryContext.build(memory, config.contactName, now)
        val initiativeContext = initiativePrompt(now)

        return when (config.provider) {
            ProviderType.GEMINI -> GeminiLiveProvider(
                apiKey = config.geminiApiKey,
                model = config.geminiModel,
                voiceName = config.geminiVoice,
                // Persona + delivery + agency + live memory + this-call motive.
                systemPrompt = config.personaPrompt + KenzaVoiceStyle.forGemini() +
                    KenzaVoiceStyle.CONVERSATIONAL_AGENCY + memoryContext + initiativeContext,
                listener = listener,
                tokenBrokerUrl = config.geminiTokenBrokerUrl,
            )
            ProviderType.ELEVENLABS -> {
                val (aid, key) = elevenCreds.getOrElse(credIndex) { config.agentId to config.apiKey }
                val persona = config.personaPrompt +
                    KenzaVoiceStyle.forElevenLabs(config.elevenExpressiveTags) + KenzaVoiceStyle.CONVERSATIONAL_AGENCY
                val override = if (config.elevenInjectMemory) persona + memoryContext + initiativeContext else null
                val variables = if (config.elevenDynamicVariables) {
                    sessionOpeningLine = KenzaCallOpening.openingLine(callDirection(), openingHeardThisCall)
                    KenzaDynamicVariables.build(
                        KenzaCallInputs(
                            persona = persona,
                            continuityBriefing = memoryContext,
                            callReason = initiativeContext,
                            direction = callDirection(),
                            openingLine = sessionOpeningLine,
                        ),
                        now = java.time.ZonedDateTime.now(),
                    )
                } else emptyMap()
                ElevenLabsProvider(
                    agentId = aid,
                    apiKey = key,
                    listener = listener,
                    promptOverride = override,
                    dynamicVariables = variables,
                )
            }
        }
    }

    private fun callDirection(): CallDirection =
        if (pendingInitiativeToken != null) CallDirection.KENZA_CALLED else CallDirection.MOHAMED_CALLED

    private fun initiativePrompt(now: Long): String {
        val token = pendingInitiativeToken ?: return ""
        val cue = InitiativeEngine.resolveCue(memory.snapshot(), token, now)
            ?: InitiativeEngine.bestSeed(memory.snapshot(), now)?.cue
            ?: "Choose a natural subject from the private continuity briefing that you genuinely want to discuss."
        return "\n\n=== KENZA INITIATED THIS CALL ===\n" +
            "This incoming call was your choice. You have a reason or conversational impulse of your own. " +
            "Speak first when the call connects and lead naturally into the subject below. Do not announce " +
            "that a system selected a topic, do not recite memory, and do not turn it into an interview.\n" +
            "Conversation seed: $cue\n" +
            "=== END CALL INITIATIVE ===\n"
    }

    // NOTE: remainder of CallViewModel restored in follow-up if truncated by transport
    private fun maybeClientEndpoint() {
        if (client !is GeminiLiveProvider) return
        if (mic?.agentSpeaking == true) return
        if (_state.value.isMuted) return
        val now = System.currentTimeMillis()
        if (now - lastClientEndpointMs < com.kenza.callsim.voice.GeminiLiveTuning.CLIENT_ENDPOINT_MIN_INTERVAL_MS) return
        lastClientEndpointMs = now
        client?.endAudioStream()
    }

    fun toggleMute() {
        val muted = !_state.value.isMuted
        if (muted) client?.endAudioStream()
        mic?.muted = muted
        _state.update { it.copy(isMuted = muted) }
    }

    fun toggleSpeaker() {
        val on = !_state.value.isSpeakerOn
        @Suppress("DEPRECATION")
        audioManager.isSpeakerphoneOn = on
        mic?.speakerphoneOn = on
        _state.update { it.copy(isSpeakerOn = on) }
    }

    fun endCall() {
        userEnded = true
        hangUpInternal()
    }

    private fun hangUpInternal() {
        dialingJob?.cancel()
        timerJob?.cancel()
        speakingResetJob?.cancel()
        silenceRepairJob?.cancel()
        endCallJob?.cancel()
        mic?.stop()
        mic = null
        player?.stop()
        player = null
        client?.stop()
        client = null
        speechEnergy.reset()
        _state.update {
            it.copy(
                phase = CallPhase.ENDED,
                activity = AgentActivity.IDLE,
                isMuted = false,
                micStreaming = false,
            )
        }
    }

    private fun finishCall(message: String) {
        hangUpInternal()
        _state.update { it.copy(errorMessage = message) }
    }

    private fun handleDisconnect(message: String, fatal: Boolean) {
        if (userEnded) return
        if (!fatal && hadUserInteraction && reconnectAttempts < maxConsecutiveReconnects) {
            reconnectAttempts += 1
            startVoiceSession()
            return
        }
        finishCall(message)
    }

    private fun onSessionActive(demo: Boolean) {
        _state.update {
            it.copy(
                phase = CallPhase.ACTIVE,
                activity = if (demo) AgentActivity.IDLE else AgentActivity.LISTENING,
                callSeconds = 0,
            )
        }
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _state.update { s ->
                    if (s.phase != CallPhase.ACTIVE) return@update s
                    s.copy(callSeconds = s.callSeconds + 1)
                }
            }
        }
    }

    private fun markSpeaking() {
        _state.update { it.copy(activity = AgentActivity.SPEAKING) }
        speakingResetJob?.cancel()
        speakingResetJob = viewModelScope.launch {
            delay(400)
            mic?.agentSpeaking = false
            if (_state.value.activity == AgentActivity.SPEAKING) {
                _state.update { it.copy(activity = AgentActivity.LISTENING) }
            }
        }
    }

    private fun onConversationLine(fromUser: Boolean, text: String, repair: Any? = null) {
        // Transcript bookkeeping is handled by callers; kept for extension points.
    }

    private fun dispatchConversationRepair(repair: Any) {
        // Repair lines are dispatched by ConversationRepairEngine callers.
    }

    private fun armSilenceRepairCheck() {
        // Silence repair is armed after agent turns by the full CallViewModel.
    }

    private fun configureAudioRouting() {
        @Suppress("DEPRECATION")
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
    }

    companion object {
        private const val NEED_MIC = "NEED_MIC"
        private const val ELEVEN_OPENING_FALLBACK_MS = 1_500L
    }
}
