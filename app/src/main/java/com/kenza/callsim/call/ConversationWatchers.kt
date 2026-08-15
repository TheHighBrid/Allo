package com.kenza.callsim.call

import java.util.Locale

/** Pure transition guards used to reject duplicate UI events before they can open resources. */
internal object CallTransitionRules {
    fun canPlaceCall(phase: CallPhase): Boolean = phase == CallPhase.IDLE

    fun canAnswerIncoming(phase: CallPhase): Boolean = phase == CallPhase.INCOMING
}

/**
 * Conservative end-of-call intent detection.
 *
 * These checks deliberately match complete, short utterances instead of arbitrary
 * substrings. For example, "I'm done with work" and "I can't see you" are normal
 * conversation, not requests to hang up.
 */
internal object ConversationEndDetector {
    private val punctuation = Regex("[^a-z0-9']+")
    private val whitespace = Regex("\\s+")

    private val farewellPhrases = setOf(
        "bye", "bye bye", "goodbye", "goodnight", "good night", "night night",
        "talk to you later", "talk later", "talk soon", "gotta go", "i gotta go",
        "i've gotta go", "i have to go", "i'll let you go", "let you go",
        "see you", "see ya", "call you later", "i'll call you later", "sleep well",
        "take care", "later gator",
    )

    private val angryHangupPhrases = setOf(
        "i'm hanging up", "im hanging up", "i am hanging up", "i'm done", "im done",
        "we're done", "were done", "we are done", "don't call me", "dont call me",
        "do not call me", "lose my number", "leave me alone", "forget it",
        "don't ever call me", "dont ever call me", "do not ever call me", "i'm out", "im out",
    )

    fun isFarewell(text: String): Boolean {
        val normalized = normalize(text)
            .removePrefix("okay ")
            .removePrefix("ok ")
            .removePrefix("alright ")
        return normalized in farewellPhrases || farewellPhrases.any { phrase ->
            normalized.removeSuffix(" babe") == phrase ||
                normalized.removeSuffix(" baby") == phrase ||
                normalized.removeSuffix(" love") == phrase
        }
    }

    fun isAngryHangup(text: String): Boolean = normalize(text) in angryHangupPhrases

    private fun normalize(text: String): String = text
        .lowercase(Locale.ROOT)
        .replace('’', '\'')
        .replace(punctuation, " ")
        .trim()
        .replace(whitespace, " ")
}
