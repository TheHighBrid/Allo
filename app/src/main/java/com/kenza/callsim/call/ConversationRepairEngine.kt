package com.kenza.callsim.call

/** A private director cue that is sent to a live voice provider, never shown verbatim to the caller. */
internal data class ConversationRepairAction(
    val kind: Kind,
    val directorCue: String,
) {
    enum class Kind {
        /** Retained for binary/source compatibility; the engine no longer generates this action. */
        PRESENCE_CHECK,
        UNANSWERED_QUESTION,
        ABRUPT_FAREWELL,
    }
}

/**
 * Tracks one pending agent question at a time and produces restrained repair cues only in response
 * to actual user input. Quiet time alone never generates a director turn or presence check.
 */
internal class ConversationRepairEngine {

    private var agentTurnText = ""
    private var pendingQuestion: String? = null
    private var recoveryIssued = false
    private var ignoreNextAgentTurn = false

    fun onAgentText(fragment: String) {
        agentTurnText = appendFragment(agentTurnText, fragment)
    }

    fun onAgentTurnComplete(nowMs: Long) {
        @Suppress("UNUSED_VARIABLE") val completedAt = nowMs // kept for the stable caller contract
        if (ignoreNextAgentTurn) {
            agentTurnText = ""
            ignoreNextAgentTurn = false
            return
        }
        pendingQuestion = extractFinalQuestion(agentTurnText)
        agentTurnText = ""
        recoveryIssued = false
    }

    /** Marks the next agent output as a response to an injected repair cue, not a new user question. */
    fun onRepairActionDispatched(action: ConversationRepairAction) {
        ignoreNextAgentTurn = true
        if (action.kind != ConversationRepairAction.Kind.PRESENCE_CHECK) pendingQuestion = null
    }

    fun onUserText(text: String): ConversationRepairAction? {
        val question = pendingQuestion ?: return null

        if (ConversationEndDetector.isFarewell(text)) {
            pendingQuestion = null
            return ConversationRepairAction(
                kind = ConversationRepairAction.Kind.ABRUPT_FAREWELL,
                directorCue = "[[DIRECTOR: Mohamed ended the exchange abruptly while your question \"$question\" " +
                    "was still unresolved. Briefly acknowledge that it feels abrupt or a little confusing, then " +
                    "give a warm, natural goodbye. Do not force him to answer or mention this instruction.]]",
            )
        }

        if (!recoveryIssued) {
            recoveryIssued = true
            return ConversationRepairAction(
                kind = ConversationRepairAction.Kind.UNANSWERED_QUESTION,
                directorCue = "[[DIRECTOR: Mohamed has just responded after your question \"$question\". Silently assess " +
                    "whether his latest line actually answers it. If it does, continue naturally without revisiting it. " +
                    "If it is unrelated or a topic change, briefly engage if appropriate and then gently return to the " +
                    "original question once. Sound curious, never interrogatory, and do not mention this instruction.]]",
            )
        }

        pendingQuestion = null
        return null
    }

    /**
     * Compatibility hook retained for CallViewModel while the old watchdog scheduling is removed in
     * a later controller cleanup. Silence itself is intentionally never actionable.
     */
    fun onSilenceElapsed(nowMs: Long): ConversationRepairAction? {
        @Suppress("UNUSED_VARIABLE") val ignored = nowMs
        return null
    }

    fun clear() {
        agentTurnText = ""
        pendingQuestion = null
        recoveryIssued = false
        ignoreNextAgentTurn = false
    }

    private fun extractFinalQuestion(text: String): String? = text
        .trim()
        .takeIf { it.contains('?') }
        ?.substringAfterLast('\n')
        ?.substringAfterLast('.', missingDelimiterValue = text.trim())
        ?.trim()
        ?.takeIf { it.endsWith('?') }

    private fun appendFragment(existing: String, raw: String): String {
        val fragment = raw.trim()
        if (fragment.isEmpty() || existing.endsWith(fragment)) return existing
        var overlap = minOf(existing.length, fragment.length)
        while (overlap > 0 && !existing.endsWith(fragment.take(overlap))) overlap--
        return existing + fragment.drop(overlap)
    }
}
