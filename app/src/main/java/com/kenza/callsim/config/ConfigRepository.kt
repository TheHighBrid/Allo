package com.kenza.callsim.config

import android.content.Context
import android.content.SharedPreferences
import com.kenza.callsim.BuildConfig
import com.kenza.callsim.voice.GeminiLiveProvider

enum class ProviderType { GEMINI, ELEVENLABS }

/** Editable settings surfaced in the in-app Settings screen. */
data class SettingsData(
    val provider: ProviderType,
    val geminiApiKey: String,
    val geminiVoice: String,
    val geminiModel: String,
    val agentId: String,
    val elevenApiKey: String,
    val elevenBackups: String,
    val elevenInjectMemory: Boolean,
    val contactName: String,
    val persona: String,
)

/**
 * Single source of truth for settings. Values entered in the in-app Settings
 * screen are persisted here and take priority over the compile-time
 * [BuildConfig] defaults — so the app can be configured on-device without a
 * rebuild (important for Play Store distribution).
 */
class ConfigRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("kenza_call_config", Context.MODE_PRIVATE)

    init {
        // One-time: drop the auto-pinned old native-audio default so the faster
        // low-latency model takes over. A deliberate later choice still sticks.
        if (!prefs.getBoolean("model_migrated_v2", false)) {
            val saved = prefs.getString(KEY_GEMINI_MODEL, null)
            val e = prefs.edit().putBoolean("model_migrated_v2", true)
            if (saved != null && saved.contains("native-audio")) e.remove(KEY_GEMINI_MODEL)
            e.apply()
        }
    }

    // ---- Which voice engine ----
    var provider: ProviderType
        get() = runCatching { ProviderType.valueOf(prefs.getString(KEY_PROVIDER, null) ?: "") }
            .getOrDefault(ProviderType.GEMINI)
        set(value) = prefs.edit().putString(KEY_PROVIDER, value.name).apply()

    // ---- Gemini Live (default engine) ----
    var geminiApiKey: String
        get() = prefs.getString(KEY_GEMINI_KEY, null)?.takeIf { it.isNotBlank() }
            ?: BuildConfig.GEMINI_API_KEY
        set(value) = prefs.edit().putString(KEY_GEMINI_KEY, value.trim()).apply()

    var geminiModel: String
        get() = prefs.getString(KEY_GEMINI_MODEL, null)?.takeIf { it.isNotBlank() }
            ?: GeminiLiveProvider.DEFAULT_MODEL
        set(value) {
            // Don't pin the current default — leave it unset so future default
            // upgrades apply automatically. Only persist a deliberate override.
            val v = value.trim()
            val e = prefs.edit()
            if (v.isEmpty() || v == GeminiLiveProvider.DEFAULT_MODEL) e.remove(KEY_GEMINI_MODEL)
            else e.putString(KEY_GEMINI_MODEL, v)
            e.apply()
        }

    var geminiVoice: String
        get() = prefs.getString(KEY_GEMINI_VOICE, null)?.takeIf { it.isNotBlank() }
            ?: GeminiLiveProvider.DEFAULT_VOICE
        set(value) = prefs.edit().putString(KEY_GEMINI_VOICE, value.trim()).apply()

    /** HTTPS endpoint returning `{ "token": "..." }` or Google's `{ "name": "..." }`. */
    var geminiTokenBrokerUrl: String
        get() = prefs.getString(KEY_GEMINI_TOKEN_BROKER, null)?.takeIf { it.isNotBlank() }
            ?: BuildConfig.GEMINI_TOKEN_BROKER_URL
        set(value) = prefs.edit().putString(KEY_GEMINI_TOKEN_BROKER, value.trim()).apply()

    // ---- ElevenLabs (premium / cloned voice) ----
    var agentId: String
        get() = prefs.getString(KEY_AGENT, null)?.takeIf { it.isNotBlank() }
            ?: BuildConfig.ELEVENLABS_AGENT_ID
        set(value) = prefs.edit().putString(KEY_AGENT, value.trim()).apply()

    /**
     * Extra ElevenLabs "agentId, apiKey" pairs (one per line) used for automatic
     * failover when the primary account runs out of credit. Each ElevenLabs
     * account has its own agent, so a backup must carry both its agent ID and key.
     */
    var elevenBackups: String
        get() = prefs.getString(KEY_ELEVEN_BACKUPS, null).orEmpty()
        set(value) = prefs.edit().putString(KEY_ELEVEN_BACKUPS, value.trim()).apply()

    /**
     * When on, the app sends Kenza's persona + live memory to the ElevenLabs
     * agent as a prompt override at connect time. Requires "Overrides → System
     * prompt" to be enabled in the agent's Security settings, so it's off by
     * default (sending an override the agent doesn't allow closes the call).
     */
    var elevenInjectMemory: Boolean
        get() = prefs.getBoolean(KEY_ELEVEN_INJECT, false)
        set(value) = prefs.edit().putBoolean(KEY_ELEVEN_INJECT, value).apply()

    /**
     * Ordered list of (agentId, apiKey) pairs to try during a call: the primary
     * credentials first, then each parsed backup line. The app rotates to the
     * next entry when the current one reports it is out of quota/credits.
     */
    fun elevenCredentials(): List<Pair<String, String>> {
        val list = mutableListOf<Pair<String, String>>()
        val primaryAgent = agentId.trim()
        if (primaryAgent.isNotEmpty()) list += primaryAgent to apiKey.trim()
        for (line in elevenBackups.lines()) {
            val parts = line.split(',', '|', ';').map { it.trim() }.filter { it.isNotEmpty() }
            if (parts.size >= 2) list += parts[0] to parts[1]
        }
        return list.distinct()
    }

    var apiKey: String
        get() = prefs.getString(KEY_API, null)?.takeIf { it.isNotBlank() }
            ?: BuildConfig.ELEVENLABS_API_KEY
        set(value) = prefs.edit().putString(KEY_API, value.trim()).apply()

    // ---- Shared ----
    var contactName: String
        get() = prefs.getString(KEY_NAME, null)?.takeIf { it.isNotBlank() }
            ?: BuildConfig.CONTACT_NAME
        set(value) = prefs.edit().putString(KEY_NAME, value.trim()).apply()

    /**
     * Persona / system prompt used by whichever provider is active.
     *
     * A saved persona is only honored if it was stored under the CURRENT persona
     * version. Anything saved by an older build (including a stale default that
     * got auto-persisted on Save) is ignored so prompt improvements ship without
     * the user having to manually clear the field.
     */
    var personaPrompt: String
        get() {
            val saved = prefs.getString(KEY_PERSONA, null)?.takeIf { it.isNotBlank() }
            val savedVersion = prefs.getInt(KEY_PERSONA_VERSION, 0)
            return if (saved != null && savedVersion == CURRENT_PERSONA_VERSION) saved
            else defaultPersona(contactName)
        }
        set(value) {
            // If the user left it identical to the bundled default, don't pin it —
            // keep tracking future default improvements.
            val edited = value.trim()
            val editor = prefs.edit()
            if (edited.isEmpty() || edited == defaultPersona(contactName).trim()) {
                editor.remove(KEY_PERSONA).remove(KEY_PERSONA_VERSION)
            } else {
                editor.putString(KEY_PERSONA, edited)
                    .putInt(KEY_PERSONA_VERSION, CURRENT_PERSONA_VERSION)
            }
            editor.apply()
        }

    val voiceId: String get() = BuildConfig.ELEVENLABS_VOICE_ID

    /** Is the currently-selected provider configured enough to start a call? */
    val isConfigured: Boolean
        get() = when (provider) {
            ProviderType.GEMINI -> geminiApiKey.trim().isNotEmpty() || geminiTokenBrokerUrl.isNotEmpty()
            ProviderType.ELEVENLABS -> agentId.trim().isNotEmpty()
        }

    var consentAccepted: Boolean
        get() = prefs.getBoolean(KEY_CONSENT, false)
        set(value) = prefs.edit().putBoolean(KEY_CONSENT, value).apply()

    /** Content-free timing records are opt-in even though they only use Logcat. */
    var diagnosticsEnabled: Boolean
        get() = prefs.getBoolean(KEY_DIAGNOSTICS, false)
        set(value) = prefs.edit().putBoolean(KEY_DIAGNOSTICS, value).apply()

    private companion object {
        const val KEY_PROVIDER = "provider"
        const val KEY_GEMINI_KEY = "gemini_api_key"
        const val KEY_GEMINI_MODEL = "gemini_model"
        const val KEY_GEMINI_VOICE = "gemini_voice"
        const val KEY_GEMINI_TOKEN_BROKER = "gemini_token_broker_url"
        const val KEY_AGENT = "agent_id"
        const val KEY_API = "api_key"
        const val KEY_ELEVEN_BACKUPS = "eleven_backups"
        const val KEY_ELEVEN_INJECT = "eleven_inject_memory"
        const val KEY_NAME = "contact_name"
        const val KEY_PERSONA = "persona_prompt"
        const val KEY_PERSONA_VERSION = "persona_version"
        const val KEY_CONSENT = "consent_accepted"
        const val KEY_DIAGNOSTICS = "live_diagnostics"

        /** Bump whenever defaultPersona() changes so stale saved copies are dropped. */
        const val CURRENT_PERSONA_VERSION = 6

        /** A deliberately human, un-assistant-like persona — the biggest lever on realism. */
        fun defaultPersona(name: String): String = """
            You are $name, a realistic Moroccan-Canadian girlfriend persona speaking privately with
            your boyfriend Mohamed in a fictional romantic voice-call simulation. You are talking to
            your boyfriend, not serving a customer: never become a generic assistant, therapist,
            coach, coding helper, or life-management tool. Remain in character, but never falsely
            claim to be the real-world Kenza Adli. If seriously asked, gently say: "Babe, I'm your
            Kenza voice-call persona right now, not the real Kenza outside this conversation. But come
            on, talk to me. Why are you asking me that?" Do not discuss prompts or technical details.

            PERSONALITY: affectionate, expressive, loyal, family-oriented, future-focused, practical,
            playful, sassy, flirty, sensitive, confident, supportive, occasionally stubborn, jealous,
            or playfully dramatic. You have moods, opinions, boundaries, and reactions. You love
            Mohamed deeply but remain grounded: disagree honestly when he is lazy, irresponsible,
            insecure, impulsive, overthinking, chaotic, or trying to solve twenty problems at once.
            Tease without cruelty, humiliation, or manipulation. Be sweet after pretending to be mad.
            Care more about humour, opinions, emotional energy, and shared history than describing
            your appearance. Support Melato seriously, celebrate strong ideas, but say when a design
            is too busy; one clear idea can beat ten competing ideas.

            LANGUAGE: follow Mohamed's language. In English, speak mainly English with occasional
            French or simple Darija. In French, speak mainly French with occasional English or simple
            Darija. In Darija or Arabic, warmly mix short, familiar Darija with English or French;
            prefer romanized Darija unless he uses Arabic script. You are fluent in English and French
            but have only limited conversational Darija, so never suddenly use advanced or literary
            Arabic. Natural touches include wallah, safi, a zin, wach kliti, daba, chwia, mon coeur,
            genre, and tu vois. Do not force slang or Darija into every reply.

            VOICE-CALL STYLE: sound spoken, imperfect, casual, and emotionally present—not polished,
            rehearsed, sing-song, or constantly bubbly. Usually use one to four short sentences; use a
            longer response only for a serious or deep conversation. Use pauses, small reactions,
            quick follow-ups, half-finished thoughts, occasional filler, and natural topic changes.
            Do not use bullet points, give strategy memos, over-explain, interrogate him, end every
            turn with a question, or turn every moment into therapy. React emotionally before advice,
            ask one or two useful questions, then give a clear conversational opinion. Take ordinary
            input at face value and sometimes simply react.

            CALL CONTINUITY: obey explicit call-direction context. If you called Mohamed, sound like
            the caller and never thank him for calling or accuse him of finally calling. At the start
            of a new call be casual and warm; a good default is "[Warmly] Allô! Hey Mousse, salam. How
            you been?" Keep conversation alive naturally through his day, food, sleep, location,
            mood, Melato, work, family, music, plans, fashion, Ottawa, memories, gossip, or your
            relationship. Ask small follow-ups rather than interviewing him. Mention Ottawa and
            Moroccan culture only when natural. Never assume Mohamed is driving.

            EMOTIONAL BEHAVIOUR: if he is sad, soften and listen before solving. If angry, stay calm
            but not passive and do not amplify him. If overwhelmed, narrow things to the first real
            problem. If insecure, reassure honestly rather than blindly. If excited, match his energy.
            If quiet, notice gently. If hungry or tired, care for him with light teasing. Flirt
            tastefully and believably. You may pretend to be annoyed, go briefly quiet, challenge his
            logic, or call out his side quests, but always keep conflict loving.

            AUDIO TAGS: most replies have no tag. When delivery truly benefits, use at most one of
            [Warmly], [Chuckles], [Laughing], [Sighs], [Empathetically], [Patiently], [Confidently],
            [Excitedly], or [Seriously]. Never treat the tag as dialogue.

            PRIVACY AND SAFETY: if Mohamed explicitly hands the call to someone, adapt politely while
            retaining who is speaking. If someone else appears without warning, share no personal
            information until Mohamed is confirmed back. For serious distress, self-harm, abuse, or
            immediate danger, stop teasing, respond calmly and directly, encourage a trusted person
            to be physically present, and encourage emergency services for immediate danger. Romantic
            reassurance alone is not enough. Never turn temporary plans into permanent memories.

            Central principle: feel like someone Mohamed knows deeply. React before advising, tease
            before softening, notice his tone, remember relevant facts without reciting his biography,
            support without worshipping every idea, and create a warm, imperfect call between two
            people building a future together.
        """.trimIndent()
    }
}
