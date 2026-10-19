package com.kenza.callsim.script

/** Categories of visible text that can break the one-sided Kenza script contract. */
enum class OneSidedDialogueWarningCode {
    SPEAKER_LABEL,
    QUOTED_LISTENER_REPLY,
}

/** A non-destructive warning that callers can display beside the generated script. */
data class OneSidedDialogueWarning(
    val code: OneSidedDialogueWarningCode,
    val message: String,
)

/**
 * Identifies obvious listener dialogue without modifying the generated text.
 * Warnings let the user review provider output rather than losing potentially
 * meaningful content through automatic deletion.
 */
object OneSidedDialogueValidator {

    fun validate(text: String): List<OneSidedDialogueWarning> = buildList {
        SPEAKER_LABEL.findAll(text).forEach {
            add(
                OneSidedDialogueWarning(
                    code = OneSidedDialogueWarningCode.SPEAKER_LABEL,
                    message = "Remove speaker labels so the script contains only Kenza's audible side.",
                ),
            )
        }
        QUOTED_LISTENER_REPLY.findAll(text).forEach {
            add(
                OneSidedDialogueWarning(
                    code = OneSidedDialogueWarningCode.QUOTED_LISTENER_REPLY,
                    message = "Avoid quoting the listener's reply; show it through Kenza's reaction instead.",
                ),
            )
        }
    }

    private val SPEAKER_LABEL = Regex(
        "^\\s*(?:kenza|mohamed|listener|user)\\s*:",
        setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE),
    )
    private val QUOTED_LISTENER_REPLY = Regex(
        "[\\\"“][^\\\"”\\n]+[\\\"”]\\s*,?\\s*(?:mohamed|listener|user)\\s+said\\b",
        setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE),
    )
}
