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
    val mood: String = "",
    val topicsText: String = "",
    val selectedMemoryIds: List<String> = emptyList(),
    val affection: IntensityLevel = IntensityLevel.MODERATE,
    val humor: IntensityLevel = IntensityLevel.MODERATE,
    val flirtation: IntensityLevel = IntensityLevel.LOW,
    val boundariesText: String = "",
    val endingStyle: String = "",
    val scriptText: String,
    val generatedTitle: String?,
    val memoryIdsUsed: List<String> = emptyList(),
)

/** A pure, versioned codec that can be unit tested without Android framework classes. */
object ScriptStudioDraftCodec {

    fun encode(draft: ScriptStudioDraft): String = listOf(
        VERSION,
        draft.requestedMinutes.toString(),
        draft.mode.name,
        encodeField(draft.language),
        encodeField(draft.callReason),
        encodeField(draft.mood),
        encodeField(draft.topicsText),
        encodeField(draft.selectedMemoryIds.joinToString("\n")),
        draft.affection.name,
        draft.humor.name,
        draft.flirtation.name,
        encodeField(draft.boundariesText),
        encodeField(draft.endingStyle),
        encodeField(draft.scriptText),
        encodeField(draft.generatedTitle.orEmpty()),
        encodeField(draft.memoryIdsUsed.joinToString("\n")),
    ).joinToString(SEPARATOR)

    fun decode(encoded: String): ScriptStudioDraft? = runCatching {
        val fields = encoded.split(SEPARATOR)
        when (fields.firstOrNull()) {
            VERSION -> decodeV2(fields)
            LEGACY_VERSION -> decodeV1(fields)
            else -> null
        }
    }.getOrNull()

    private fun decodeV2(fields: List<String>): ScriptStudioDraft {
        require(fields.size == FIELD_COUNT)
        return ScriptStudioDraft(
            requestedMinutes = fields[1].toInt(),
            mode = ScriptMode.valueOf(fields[2]),
            language = decodeField(fields[3]),
            callReason = decodeField(fields[4]),
            mood = decodeField(fields[5]),
            topicsText = decodeField(fields[6]),
            selectedMemoryIds = decodeList(fields[7]),
            affection = runCatching { IntensityLevel.valueOf(fields[8]) }.getOrDefault(IntensityLevel.MODERATE),
            humor = runCatching { IntensityLevel.valueOf(fields[9]) }.getOrDefault(IntensityLevel.MODERATE),
            flirtation = runCatching { IntensityLevel.valueOf(fields[10]) }.getOrDefault(IntensityLevel.LOW),
            boundariesText = decodeField(fields[11]),
            endingStyle = decodeField(fields[12]),
            scriptText = decodeField(fields[13]),
            generatedTitle = decodeField(fields[14]).ifBlank { null },
            memoryIdsUsed = decodeList(fields[15]),
        )
    }

    /** Existing v1 drafts migrate forward without losing user text. */
    private fun decodeV1(fields: List<String>): ScriptStudioDraft {
        require(fields.size == LEGACY_FIELD_COUNT)
        return ScriptStudioDraft(
            requestedMinutes = fields[1].toInt(),
            mode = ScriptMode.valueOf(fields[2]),
            language = decodeField(fields[3]),
            callReason = decodeField(fields[4]),
            scriptText = decodeField(fields[5]),
            generatedTitle = decodeField(fields[6]).ifBlank { null },
        )
    }

    private fun encodeField(value: String): String = Base64.getEncoder()
        .encodeToString(value.toByteArray(StandardCharsets.UTF_8))

    private fun decodeField(value: String): String = String(
        Base64.getDecoder().decode(value),
        StandardCharsets.UTF_8,
    )

    private fun decodeList(value: String): List<String> = decodeField(value)
        .lines()
        .map(String::trim)
        .filter(String::isNotBlank)
        .distinct()

    private const val VERSION = "v2"
    private const val LEGACY_VERSION = "v1"
    private const val SEPARATOR = "."
    private const val FIELD_COUNT = 16
    private const val LEGACY_FIELD_COUNT = 7
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
