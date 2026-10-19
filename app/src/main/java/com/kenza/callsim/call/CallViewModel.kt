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
import com.kenza.callsim.memory.MemoryStore
import com.kenza.callsim.schedule.IncomingCallService
import com.kenza.callsim.schedule.ScheduledCallRuntime
import com.kenza.callsim.voice.AudioRoutePolicy
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

    private val memory = MemoryStore(app)
    private val initiativeStore = InitiativeStore(app)
    private val transcript = mutableListOf<Pair<String, String>>()
    private var callStartedAt = 0L

    private var pendingInitiativeToken: String? = null
    private var initiativeOpeningSent = false

    private var openingHeardThisCall = false
    private var sessionAgentAudioHeard = false
    private var sessionOpeningLine = ""
    private var sessionProviderName = ""

    private var endCallJob: Job? = null
    private var silenceRepairJob: Job? = null

    private var timerJob: Job? = null
    private var speakingResetJob: Job? = null
    private var dialingJob: Job? = null
    private var pendingStartAfterPermission = false

    private var userEnded = false
    private var reconnectAttempts = 0
    private val maxConsecutiveReconnects = 4
    private var hadUserInteraction = false

    private var elevenCreds: List<Pair<String, String>> = emptyList()
    private var credIndex = 0

    init {
        AudioRoutePolicy.earpieceFullDuplexEnabled = config.earpieceFullDuplex
    }

    fun simulateIncomingCall() {
        if (!CallTransitionRules.canPlaceCall(_state.value.phase)) return
        if (!ScheduledCallRuntime.tryStartUserInitiatedCall()) return
        armIncomingInitiative(explicitToken = null, countAgainstCooldown = false)
        _state.update { it.copy(phase = CallPhase.INCOMING, errorMessage = null) }
        ringtone.start()
    }

    fun onScheduledIncomingCall(initiativeToken: String? = null) {
        val phase = _state.value.phase
        if (phase == CallPhase.INCOMING) return
        if (phase != CallPhase.IDLE && phase != CallPhase.ENDED) {
            IncomingCallService.stop(getApplication())
            return
        }
        armIncomingInitiative(initiativeToken, countAgainstCooldown = initiativeToken == null)
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
        pendingInitiativeToken = null
        initiativeOpeningSent = false
        ringtone.stop()
        _state.update { it.copy(phase = CallPhase.DIALING, errorMessage = null) }
        dialingJob?.cancel()
        dialingJob = viewModelScope.launch { beginConnecting() }
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
        if (!config.isConfigured) onSessionActive(demo = true) else ensureMicThenStart()
    }

    private fun ensureMicThenStart() {
        val ctx = getApplication<Application>()
        val granted = ctx.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        if (granted) startVoiceSession()
        else {
            pendingStartAfterPermission = true
            _state.update { it.copy(errorMessage = NEED_MIC) }
        }
    }

    fun onMicPermissionResult(granted: Boolean) {
        if (_state.value.errorMessage == NEED_MIC) _state.update { it.copy(errorMessage = null) }
        if (!pendingStartAfterPermission) return
        pendingStartAfterPermission = false
        if (granted) startVoiceSession()
        else finishCall("Microphone permission is required for the call.")
    }

    private fun startVoiceSession() {
        if (!config.isConfigured) {
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
                if (player == null) {
                    player = PcmPlayer(outputSampleRate, telemetry.playback).also { it.start() }
                }
                if (mic == null) {
                    speechEnergy.reset()
                    mic = MicRecorder(
                        onChunk = { chunk ->
                            when (speechEnergy.onPcm(chunk)) {
                                LocalSpeechEnergyTracker.Event.START -> telemetry.localSpeechStart()
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
            "This incoming call was your choice. Speak first when the call connects.\n" +
            "Conversation seed: $cue\n=== END CALL INITIATIVE ===\n"
    }

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

    fun toggleKeypad() {
        _state.update { it.copy(isKeypadVisible = !it.isKeypadVisible) }
    }

    fun pressKeypadKey(key: Char) {
        dtmf.play(key)
        appendDigit(key)
    }

    fun appendDigit(digit: Char) {
        _state.update { it.copy(dialedNumber = (it.dialedNumber + digit).take(32)) }
    }

    fun deleteDigit() {
        _state.update {
            if (it.dialedNumber.isEmpty()) it
            else it.copy(dialedNumber = it.dialedNumber.dropLast(1))
        }
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
        mic?.stop(); mic = null
        player?.stop(); player = null
        client?.stop(); client = null
        speechEnergy.reset()
        _state.update {
            it.copy(
                phase = CallPhase.ENDED,
                activity = AgentActivity.IDLE,
                isMuted = false,
                micStreaming = false,
                isKeypadVisible = false,
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
                callDurationSec = 0,
                errorMessage = if (demo) DEMO_NOTICE else null,
            )
        }
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _state.update { s ->
                    if (s.phase != CallPhase.ACTIVE) return@update s
                    s.copy(callDurationSec = s.callDurationSec + 1)
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

    private fun onConversationLine(fromUser: Boolean, text: String, repair: Any? = null) {}
    private fun dispatchConversationRepair(repair: Any) {}
    private fun armSilenceRepairCheck() {}

    private fun configureAudioRouting() {
        @Suppress("DEPRECATION")
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        AudioRoutePolicy.earpieceFullDuplexEnabled = config.earpieceFullDuplex
    }

    fun currentSettings(): SettingsData = SettingsData(
        provider = config.provider,
        geminiApiKey = config.geminiApiKey,
        geminiVoice = config.geminiVoice,
        geminiModel = config.geminiModel,
        agentId = config.agentId,
        elevenApiKey = config.apiKey,
        elevenBackups = config.elevenBackups,
        elevenInjectMemory = config.elevenInjectMemory,
        elevenDynamicVariables = config.elevenDynamicVariables,
        elevenExpressiveTags = config.elevenExpressiveTags,
        contactName = config.contactName,
        persona = config.personaPrompt,
        weatherApiKey = config.weatherApiKey,
        userCity = config.userCity,
        earpieceFullDuplex = config.earpieceFullDuplex,
    )

    fun saveSettings(data: SettingsData) {
        config.provider = data.provider
        config.geminiApiKey = data.geminiApiKey
        config.geminiVoice = data.geminiVoice
        config.geminiModel = data.geminiModel
        config.agentId = data.agentId
        config.apiKey = data.elevenApiKey
        config.elevenBackups = data.elevenBackups
        config.elevenInjectMemory = data.elevenInjectMemory
        config.elevenDynamicVariables = data.elevenDynamicVariables
        config.elevenExpressiveTags = data.elevenExpressiveTags
        config.contactName = data.contactName
        config.personaPrompt = data.persona
        config.weatherApiKey = data.weatherApiKey
        config.userCity = data.userCity
        config.earpieceFullDuplex = data.earpieceFullDuplex
        AudioRoutePolicy.earpieceFullDuplexEnabled = data.earpieceFullDuplex
        _state.update {
            it.copy(contactName = data.contactName, isConfigured = config.isConfigured)
        }
    }

    fun voiceId(): String = config.voiceId
    fun isConsentAccepted(): Boolean = config.consentAccepted
    fun acceptConsent() { config.consentAccepted = true }
    fun clearError() { _state.update { it.copy(errorMessage = null) } }

    companion object {
        const val NEED_MIC = "NEED_MIC"
        const val DEMO_NOTICE =
            "Demo mode: the selected voice provider is not configured, so the call screen works " +
                "without live voice. Open Settings to connect a provider."
        private const val ELEVEN_OPENING_FALLBACK_MS = 1_500L
    }
}
