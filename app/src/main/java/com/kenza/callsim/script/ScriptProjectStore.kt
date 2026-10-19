package com.kenza.callsim.script

import android.content.Context
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.UUID

/** Lightweight metadata shown by the Script Studio project library. */
data class ScriptProjectSummary(
    val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
)

/**
 * Bounded local project library built on the same private preferences as the current draft.
 * Draft payloads reuse [ScriptStudioDraftCodec], keeping one serialization format for the editor.
 */
class ScriptProjectStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    @Synchronized
    fun projects(): List<ScriptProjectSummary> = decodeIndex(prefs.getString(KEY_INDEX, null).orEmpty())
        .sortedByDescending { it.updatedAt }

    @Synchronized
    fun create(title: String = "Untitled script", draft: ScriptStudioDraft = emptyDraft()): String {
        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()
        val cleanTitle = title.trim().ifBlank { "Untitled script" }.take(MAX_TITLE_CHARS)
        val updated = (projects() + ScriptProjectSummary(id, cleanTitle, now, now))
            .sortedByDescending { it.updatedAt }
            .take(MAX_PROJECTS)
        val editor = prefs.edit()
            .putString(projectKey(id), ScriptStudioDraftCodec.encode(draft))
            .putString(KEY_INDEX, encodeIndex(updated))
            .putString(KEY_ACTIVE_PROJECT, id)
        // Pruning is explicit so stale project payloads do not accumulate forever.
        val keep = updated.map { it.id }.toSet()
        projects().filterNot { it.id in keep }.forEach { editor.remove(projectKey(it.id)) }
        editor.apply()
        return id
    }

    @Synchronized
    fun activate(id: String): ScriptStudioDraft? {
        if (projects().none { it.id == id }) return null
        val draft = load(id) ?: return null
        prefs.edit()
            .putString(KEY_ACTIVE_PROJECT, id)
            .putString(KEY_LEGACY_CURRENT_DRAFT, ScriptStudioDraftCodec.encode(draft))
            .apply()
        return draft
    }

    @Synchronized
    fun activeProjectId(): String? = prefs.getString(KEY_ACTIVE_PROJECT, null)
        ?.takeIf { id -> projects().any { it.id == id } }

    @Synchronized
    fun load(id: String): ScriptStudioDraft? = prefs.getString(projectKey(id), null)
        ?.let(ScriptStudioDraftCodec::decode)

    /** Called by the existing editor autosave path whenever a library project is active. */
    @Synchronized
    fun saveActiveDraft(draft: ScriptStudioDraft) {
        val id = activeProjectId() ?: return
        val now = System.currentTimeMillis()
        val current = projects()
        val updated = current.map { project ->
            if (project.id == id) project.copy(updatedAt = now) else project
        }
        prefs.edit()
            .putString(projectKey(id), ScriptStudioDraftCodec.encode(draft))
            .putString(KEY_INDEX, encodeIndex(updated))
            .apply()
    }

    @Synchronized
    fun rename(id: String, title: String): Boolean {
        val clean = title.trim().ifBlank { "Untitled script" }.take(MAX_TITLE_CHARS)
        val current = projects()
        if (current.none { it.id == id }) return false
        val now = System.currentTimeMillis()
        val updated = current.map { project ->
            if (project.id == id) project.copy(title = clean, updatedAt = now) else project
        }
        prefs.edit().putString(KEY_INDEX, encodeIndex(updated)).apply()
        return true
    }

    @Synchronized
    fun duplicate(id: String): String? {
        val source = projects().firstOrNull { it.id == id } ?: return null
        val draft = load(id) ?: return null
        return create("${source.title} copy", draft)
    }

    @Synchronized
    fun delete(id: String): Boolean {
        val current = projects()
        if (current.none { it.id == id }) return false
        val updated = current.filterNot { it.id == id }
        val editor = prefs.edit()
            .remove(projectKey(id))
            .putString(KEY_INDEX, encodeIndex(updated))
        if (activeProjectId() == id) editor.remove(KEY_ACTIVE_PROJECT)
        editor.apply()
        return true
    }

    @Synchronized
    fun clearActive() {
        prefs.edit().remove(KEY_ACTIVE_PROJECT).apply()
    }

    /** Migrates the pre-library single draft once, so existing work appears in the library. */
    @Synchronized
    fun migrateLegacyDraftIfNeeded() {
        if (prefs.getBoolean(KEY_MIGRATED, false)) return
        val legacy = prefs.getString(KEY_LEGACY_CURRENT_DRAFT, null)?.let(ScriptStudioDraftCodec::decode)
        prefs.edit().putBoolean(KEY_MIGRATED, true).apply()
        if (legacy != null && (legacy.scriptText.isNotBlank() || legacy.callReason.isNotBlank() || legacy.topicsText.isNotBlank())) {
            create(legacy.generatedTitle ?: "Recovered draft", legacy)
        }
    }

    companion object {
        fun emptyDraft() = ScriptStudioDraft(
            requestedMinutes = 10,
            mode = ScriptMode.CASUAL_DAILY,
            language = "English",
            callReason = "",
            scriptText = "",
            generatedTitle = null,
        )

        private const val PREFERENCES = "kenza_script_studio"
        private const val KEY_INDEX = "project_index_v1"
        private const val KEY_ACTIVE_PROJECT = "active_project_id"
        private const val KEY_MIGRATED = "project_library_migrated_v1"
        private const val KEY_LEGACY_CURRENT_DRAFT = "current_draft_v1"
        private const val MAX_PROJECTS = 40
        private const val MAX_TITLE_CHARS = 80

        private fun projectKey(id: String) = "project_draft_$id"

        private fun encodeIndex(projects: List<ScriptProjectSummary>): String = projects.joinToString("\n") { project ->
            listOf(
                project.id,
                project.createdAt.toString(),
                project.updatedAt.toString(),
                encode(project.title),
            ).joinToString("|")
        }

        private fun decodeIndex(raw: String): List<ScriptProjectSummary> = raw.lineSequence()
            .mapNotNull { line ->
                val fields = line.split('|')
                if (fields.size != 4) return@mapNotNull null
                val created = fields[1].toLongOrNull() ?: return@mapNotNull null
                val updated = fields[2].toLongOrNull() ?: return@mapNotNull null
                val title = runCatching { decode(fields[3]) }.getOrNull() ?: return@mapNotNull null
                ScriptProjectSummary(fields[0], title, created, updated)
            }
            .distinctBy { it.id }
            .toList()

        private fun encode(value: String): String = Base64.getEncoder()
            .encodeToString(value.toByteArray(StandardCharsets.UTF_8))

        private fun decode(value: String): String = String(
            Base64.getDecoder().decode(value),
            StandardCharsets.UTF_8,
        )
    }
}
