package com.kenza.callsim.script

import android.content.Context
import java.nio.charset.StandardCharsets
import java.util.Base64

/**
 * The complete editable Script Studio state that may safely remain on-device.
 * Provider keys, broker URLs, and all other runtime credentials are deliberately
 * absent from this model and therefore cannot enter a draft record.
 */
data class ScriptStudioDraft(
    val requestedMinutes: Int,
    val mode: ScriptMode,
    val language: String,
    val callReason: String,
    val scriptText: String,
    val generatedTitle: String?,
)

/** A pure, versioned codec that can be unit tested without Android framework classes. */
object ScriptStudioDraftCodec {

    fun encode(draft: ScriptStudioDraft): String = listOf(
        VERSION,
        draft.requestedMinutes.toString(),
        draft.mode.name,
        encodeField(draft.language),
        encodeField(draft.callReason),
        encodeField(draft.scriptText),
        encodeField(draft.generatedTitle.orEmpty()),
    ).joinToString(SEPARATOR)

    fun decode(encoded: String): ScriptStudioDraft? = runCatching {
        val fields = encoded.split(SEPARATOR)
        require(fields.size == FIELD_COUNT && fields[0] == VERSION)
        ScriptStudioDraft(
            requestedMinutes = fields[1].toInt(),
            mode = ScriptMode.valueOf(fields[2]),
            language = decodeField(fields[3]),
            callReason = decodeField(fields[4]),
            scriptText = decodeField(fields[5]),
            generatedTitle = decodeField(fields[6]).ifBlank { null },
        )
    }.getOrNull()

    private fun encodeField(value: String): String = Base64.getEncoder()
        .encodeToString(value.toByteArray(StandardCharsets.UTF_8))

    private fun decodeField(value: String): String = String(
        Base64.getDecoder().decode(value),
        StandardCharsets.UTF_8,
    )

    private const val VERSION = "v1"
    private const val SEPARATOR = "."
    private const val FIELD_COUNT = 7
}

/** Persists exactly one editable Script Studio draft in the app's private preferences. */
class ScriptStudioDraftStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun load(): ScriptStudioDraft? = prefs.getString(KEY_DRAFT, null)
        ?.let(ScriptStudioDraftCodec::decode)

    fun save(draft: ScriptStudioDraft) {
        prefs.edit().putString(KEY_DRAFT, ScriptStudioDraftCodec.encode(draft)).apply()
    }

    fun clear() {
        prefs.edit().remove(KEY_DRAFT).apply()
    }

    private companion object {
        const val PREFERENCES = "kenza_script_studio"
        const val KEY_DRAFT = "current_draft_v1"
    }
}
