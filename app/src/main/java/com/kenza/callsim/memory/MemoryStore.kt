package com.kenza.callsim.memory

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import kotlin.math.max

/**
 * Encrypted app-local memory store. It preserves durable memories, compact
 * post-call summaries, editable profiles, plans and unresolved topics. Raw call
 * transcripts are intentionally not persisted.
 */
class MemoryStore(context: Context) {

    private val appContext = context.applicationContext
    private val storage = SecureMemoryStorage(appContext)
    private val legacyPrefs = appContext.getSharedPreferences("kenza_memory", Context.MODE_PRIVATE)

    @Synchronized
    fun snapshot(): MemorySnapshot = readState()

    fun items(): List<MemoryItem> = snapshot().items

    fun callSummaries(): List<CallSummary> = snapshot().calls.sortedByDescending { it.startedAt }

    fun profiles(): PersonalityProfiles = snapshot().profiles

    @Synchronized
    fun saveProfiles(profiles: PersonalityProfiles) {
        val current = readState()
        writeState(current.copy(profiles = profiles.copy(updatedAt = System.currentTimeMillis())))
    }

    /** Compatibility method retained for existing callers. */
    @Synchronized
    fun save(list: List<MemoryItem>) {
        val state = readState()
        writeState(state.copy(items = prune(list)))
    }

    /** Adds memories while merging exact and strong near-duplicates. */
    @Synchronized
    fun addAll(new: List<MemoryItem>): List<String> {
        if (new.isEmpty()) return emptyList()
        val state = readState()
        val existing = state.items.toMutableList()
        val storedIds = mutableListOf<String>()
        val now = System.currentTimeMillis()

        for (candidate in new) {
            val clean = candidate.text.trim().replace(Regex("\\s+"), " ").take(500)
            if (clean.isBlank()) continue
            val incoming = candidate.copy(text = clean, updatedAt = max(candidate.updatedAt, now))
            val index = existing.indexOfFirst { MemoryPolicy.isNearDuplicate(it.text, clean) }
            if (index >= 0) {
                val old = existing[index]
                val merged = old.copy(
                    kind = if (incoming.kind == MemoryKind.CORRECTION) incoming.kind else old.kind,
                    owner = if (old.owner == MemoryOwner.USER) incoming.owner else old.owner,
                    text = if (incoming.kind == MemoryKind.CORRECTION) incoming.text else old.text,
                    updatedAt = now,
                    importance = max(old.importance, incoming.importance),
                    confidence = max(old.confidence, incoming.confidence),
                    dueAt = incoming.dueAt ?: old.dueAt,
                    done = if (incoming.kind == MemoryKind.CORRECTION) incoming.done else old.done,
                    pinned = old.pinned || incoming.pinned,
                    sourceCallId = incoming.sourceCallId ?: old.sourceCallId,
                )
                existing[index] = merged
                storedIds += merged.id
            } else {
                existing += incoming
                storedIds += incoming.id
            }
        }
        writeState(state.copy(items = prune(existing)))
        return storedIds.distinct()
    }

    @Synchronized
    fun addManualMemory(
        text: String,
        owner: MemoryOwner,
        kind: MemoryKind,
        importance: Int = 4,
    ): String? {
        val clean = text.trim()
        if (clean.isEmpty()) return null
        val now = System.currentTimeMillis()
        return addAll(
            listOf(
                MemoryItem(
                    id = UUID.randomUUID().toString(),
                    kind = kind,
                    owner = owner,
                    text = clean,
                    createdAt = now,
                    updatedAt = now,
                    importance = importance.coerceIn(1, 5),
                    confidence = 1.0,
                )
            )
        ).firstOrNull()
    }

    /**
     * Replaces one existing durable memory in place after an explicit user correction.
     * The original ID, creation time, source, importance and pin state remain stable, while
     * the stale text is removed immediately so retrieval can never return both versions.
     */
    @Synchronized
    fun correctMemory(id: String, replacementText: String): Boolean {
        val state = readState()
        val index = state.items.indexOfFirst { it.id == id }
        if (index < 0) return false
        val corrected = MemoryPolicy.corrected(
            item = state.items[index],
            replacementText = replacementText,
            now = System.currentTimeMillis(),
        ) ?: return false
        val items = state.items.toMutableList().also { it[index] = corrected }
        writeState(state.copy(items = prune(items)))
        return true
    }

    @Synchronized
    fun deleteMemory(id: String) {
        val state = readState()
        writeState(state.copy(items = state.items.filterNot { it.id == id }))
    }

    @Synchronized
    fun togglePinned(id: String) {
        val now = System.currentTimeMillis()
        val state = readState()
        writeState(state.copy(items = state.items.map {
            if (it.id == id) it.copy(pinned = !it.pinned, updatedAt = now) else it
        }))
    }

    @Synchronized
    fun toggleDone(id: String) {
        val now = System.currentTimeMillis()
        val state = readState()
        writeState(state.copy(items = state.items.map {
            if (it.id == id) it.copy(done = !it.done, updatedAt = now) else it
        }))
    }

    /** Starts a pending record that the extractor completes after hang-up. */
    @Synchronized
    fun recordCall(startedAt: Long) {
        val state = readState()
        if (state.calls.any { it.startedAt == startedAt }) return
        val pending = CallSummary(
            id = UUID.randomUUID().toString(),
            startedAt = startedAt,
            endedAt = System.currentTimeMillis(),
            summary = "Creating memory summary…",
            processing = true,
        )
        writeState(state.copy(calls = (state.calls + pending).sortedBy { it.startedAt }.takeLast(MAX_CALLS)))
    }

    @Synchronized
    fun completeLatestCall(
        endedAt: Long,
        summary: String,
        mood: String,
        highlights: List<String>,
        unresolvedTopics: List<String>,
        followUp: String?,
        memoryIds: List<String> = emptyList(),
        candidateMemories: List<MemoryItem> = emptyList(),
        error: String? = null,
    ) {
        val state = readState()
        val index = state.calls.indexOfLast { it.processing }
        if (index < 0) return
        val calls = state.calls.toMutableList()
        val old = calls[index]
        calls[index] = old.copy(
            endedAt = endedAt,
            summary = summary.trim().take(1600),
            mood = mood.trim().take(120),
            highlights = highlights.cleanMemoryList(8),
            unresolvedTopics = unresolvedTopics.cleanMemoryList(8),
            followUp = followUp?.trim()?.take(500),
            memoryIds = memoryIds.distinct(),
            candidateMemories = candidateMemories
                .map { it.copy(sourceCallId = old.id) }
                .distinctBy { MemoryPolicy.normalize(it.text) }
                .take(MAX_CANDIDATE_MEMORIES),
            processing = false,
            processingError = error,
        )
        writeState(state.copy(calls = calls.sortedBy { it.startedAt }.takeLast(MAX_CALLS)))
    }

    /** Promotes one reviewed candidate into durable memory and removes it from the review queue. */
    @Synchronized
    fun approveCandidateMemory(callId: String, candidateId: String): Boolean {
        val initial = readState()
        val call = initial.calls.firstOrNull { it.id == callId } ?: return false
        val candidate = call.candidateMemories.firstOrNull { it.id == candidateId } ?: return false
        val storedId = addAll(listOf(candidate.copy(sourceCallId = callId))).firstOrNull() ?: return false

        val latest = readState()
        val calls = latest.calls.map { current ->
            if (current.id != callId) current else current.copy(
                memoryIds = (current.memoryIds + storedId).distinct(),
                candidateMemories = current.candidateMemories.filterNot { it.id == candidateId },
            )
        }
        writeState(latest.copy(calls = calls))
        return true
    }

    /** Rejects one extracted candidate without ever adding it to durable memory. */
    @Synchronized
    fun rejectCandidateMemory(callId: String, candidateId: String): Boolean {
        val state = readState()
        var found = false
        val calls = state.calls.map { call ->
            if (call.id != callId || call.candidateMemories.none { it.id == candidateId }) {
                call
            } else {
                found = true
                call.copy(candidateMemories = call.candidateMemories.filterNot { it.id == candidateId })
            }
        }
        if (found) writeState(state.copy(calls = calls))
        return found
    }

    fun callTimes(): List<Long> = snapshot().calls.map { it.startedAt }.filter { it > 0 }.sorted()

    @Synchronized
    fun deleteCall(id: String) {
        val state = readState()
        writeState(state.copy(calls = state.calls.filterNot { it.id == id }))
    }

    @Synchronized
    fun clearAll() {
        storage.clear()
        legacyPrefs.edit().clear().apply()
        writeState(MemorySnapshot(updatedAt = System.currentTimeMillis()))
    }

    private fun readState(): MemorySnapshot {
        storage.read()?.let { raw ->
            runCatching { MemorySnapshot.fromJson(JSONObject(raw)) }.getOrNull()?.let {
                return ImmutableMemorySeed.apply(it)
            }
        }
        val migrated = ImmutableMemorySeed.apply(migrateLegacy())
        storage.write(migrated.toJson().toString())
        legacyPrefs.edit().clear().apply()
        return migrated
    }

    private fun writeState(state: MemorySnapshot) {
        storage.write(
            ImmutableMemorySeed.apply(state)
                .copy(updatedAt = System.currentTimeMillis())
                .toJson()
                .toString()
        )
    }

    /** Imports the v1 SharedPreferences data on first launch after upgrade. */
    private fun migrateLegacy(): MemorySnapshot {
        val items = runCatching {
            val arr = JSONArray(legacyPrefs.getString("items", "[]"))
            (0 until arr.length()).mapNotNull { index ->
                arr.optJSONObject(index)?.let(MemoryItem::fromJson)
            }
        }.getOrDefault(emptyList())
        val oldTimes = runCatching {
            val arr = JSONArray(legacyPrefs.getString("calls", "[]"))
            (0 until arr.length()).map { arr.optLong(it) }.filter { it > 0 }
        }.getOrDefault(emptyList())
        val calls = oldTimes.takeLast(MAX_CALLS).map { time ->
            CallSummary(
                id = UUID.randomUUID().toString(),
                startedAt = time,
                endedAt = time,
                summary = "Previous call. A detailed summary was not available in the older memory format.",
            )
        }
        return MemorySnapshot(items = prune(items), calls = calls, updatedAt = System.currentTimeMillis())
    }

    private fun prune(items: List<MemoryItem>): List<MemoryItem> {
    val now = System.currentTimeMillis()
    if (items.size <= MAX_MEMORIES) return items

    val sorted = items.sortedByDescending { MemoryPolicy.score(it, now) }
    val keep = sorted.take(MAX_MEMORIES)
    val cold = sorted.drop(MAX_MEMORIES).filter { !it.pinned }

    if (cold.isNotEmpty()) {
        // Trigger the compression process
        // In a real app, use a CoroutineScope (e.g., viewModelScope or a custom scope)
        scope.launch {
            val narrative = compressor.compress(cold)
            if (narrative != null) {
                addAll(listOf(narrative))
            }
        }
    }

    return keep
}
    }

    private companion object {
        const val MAX_MEMORIES = 400
        const val MAX_CALLS = 60
        const val MAX_CANDIDATE_MEMORIES = 10
    }
}
