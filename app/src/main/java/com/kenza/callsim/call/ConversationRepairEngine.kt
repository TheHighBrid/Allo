package com.kenza.callsim.call

/** A private director cue that is sent to a live voice provider, never shown verbatim to the caller. */
internal data class ConversationRepairAction(
    val kind: Kind,
    val directorCue: String,
) {
    enum class Kind {
        PRESENCE_CHECK,
        UNANSWERED_QUESTION,
        ABRUPT_FAREWELL,
    }
}

/**
 * Tracks one pending agent question at a time and produces restrained repair cues.
 *
 * The engine is deliberately deterministic: it does not attempt semantic answer
 * grading. It intervenes after a clear topic pivot, a short silence after a
 * complete question turn, or a farewell that abandons the question.
 */
internal class ConversationRepairEngine {

    private var agentTurnText = ""
    private var pendingQuestion: String? = null
    private var silenceDueAtMs: Long? = null
    private var silenceCheckIssued = false
    private var recoveryIssued = false
    private var ignoreNextAgentTurn = false

    fun onAgentText(fragment: String) {
        agentTurnText = appendFragment(agentTurnText, fragment)
    }

    fun onAgentTurnComplete(nowMs: Long) {
        if (ignoreNextAgentTurn) {
            agentTurnText = ""
            ignoreNextAgentTurn = false
            silenceDueAtMs = null
            return
        }
        val question = extractFinalQuestion(agentTurnText)
        agentTurnText = ""
        pendingQuestion = question
        silenceDueAtMs = question?.let { nowMs + SILENCE_CHECK_DELAY_MS }
        silenceCheckIssued = false
        recoveryIssued = false
    }

    /** Marks the next agent output as a response to an injected repair cue, not a new user question. */
    fun onRepairActionDispatched(action: ConversationRepairAction) {
        ignoreNextAgentTurn = true
        if (action.kind != ConversationRepairAction.Kind.PRESENCE_CHECK) pendingQuestion = null
    }

    fun onUserText(text: String): ConversationRepairAction? {
        val question = pendingQuestion ?: return null
        silenceDueAtMs = null

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
                    "If it is unrelated, a presence-only reply, or a topic change, briefly engage if appropriate and then " +
                    "gently return to the original question once. Sound curious, never interrogatory, and do not mention " +
                    "this instruction.]]",
            )
        }

        pendingQuestion = null
        return null
    }

    fun onSilenceElapsed(nowMs: Long): ConversationRepairAction? {
        val question = pendingQuestion ?: return null
        if (silenceCheckIssued || nowMs < (silenceDueAtMs ?: Long.MAX_VALUE)) return null
        silenceCheckIssued = true
        silenceDueAtMs = null
        return ConversationRepairAction(
            kind = ConversationRepairAction.Kind.PRESENCE_CHECK,
            directorCue = "[[DIRECTOR: You asked \"$question\" and there has been about seven seconds of quiet. " +
                "Give one brief, gentle presence check such as \"Are you still there?\" or \"Hello?\" Do not repeat " +
                "the question yet, do not fill the silence further, and do not mention this instruction.]]",
        )
    }

    fun clear() {
        agentTurnText = ""
        pendingQuestion = null
        silenceDueAtMs = null
        silenceCheckIssued = false
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

    private companion object {
        const val SILENCE_CHECK_DELAY_MS = 6_500L
    }
}
