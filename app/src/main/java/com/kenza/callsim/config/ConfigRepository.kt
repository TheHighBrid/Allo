package com.kenza.callsim.config

import android.content.Context
import android.content.SharedPreferences
import com.kenza.callsim.BuildConfig

/**
 * Single source of truth for the agent settings. Values entered in the in-app
 * Settings screen are persisted here and take priority over the compile-time
 * [BuildConfig] defaults — so the app can be configured on-device without a
 * rebuild (important for Play Store distribution).
 */
class ConfigRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("kenza_call_config", Context.MODE_PRIVATE)

    var agentId: String
        get() = prefs.getString(KEY_AGENT, null)?.takeIf { it.isNotBlank() }
            ?: BuildConfig.ELEVENLABS_AGENT_ID
        set(value) = prefs.edit().putString(KEY_AGENT, value.trim()).apply()

    var apiKey: String
        get() = prefs.getString(KEY_API, null)?.takeIf { it.isNotBlank() }
            ?: BuildConfig.ELEVENLABS_API_KEY
        set(value) = prefs.edit().putString(KEY_API, value.trim()).apply()

    var contactName: String
        get() = prefs.getString(KEY_NAME, null)?.takeIf { it.isNotBlank() }
            ?: BuildConfig.CONTACT_NAME
        set(value) = prefs.edit().putString(KEY_NAME, value.trim()).apply()

    val voiceId: String get() = BuildConfig.ELEVENLABS_VOICE_ID

    val isConfigured: Boolean get() = agentId.trim().isNotEmpty()

    /** First-run consent acknowledgement (voice-cloning disclaimer). */
    var consentAccepted: Boolean
        get() = prefs.getBoolean(KEY_CONSENT, false)
        set(value) = prefs.edit().putBoolean(KEY_CONSENT, value).apply()

    private companion object {
        const val KEY_AGENT = "agent_id"
        const val KEY_API = "api_key"
        const val KEY_NAME = "contact_name"
        const val KEY_CONSENT = "consent_accepted"
    }
}
