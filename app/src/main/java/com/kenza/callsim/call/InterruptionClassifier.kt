package com.kenza.callsim.call

/** Heuristic classification for evaluation; never injects a second model response. */
internal enum class InterruptionKind { ACKNOWLEDGMENT, CORRECTION, TOPIC_CHANGE, EMOTIONAL, UNCLEAR, OTHER }

internal object InterruptionClassifier {
    fun classify(text: String): InterruptionKind {
        val clean = text.trim().lowercase().replace(Regex("""[.,!?]+$"""), "")
        return when {
            clean.isBlank() -> InterruptionKind.UNCLEAR
            clean in setOf("mm", "mhm", "mm-hm", "yeah", "oui", "okay", "ok", "right", "aha") ->
                InterruptionKind.ACKNOWLEDGMENT
            Regex("""^(no|non|actually|i meant|je voulais dire|not what)\b""").containsMatchIn(clean) ->
                InterruptionKind.CORRECTION
            Regex("""^(anyway|by the way|another thing|par contre|sinon|au fait)\b""").containsMatchIn(clean) ->
                InterruptionKind.TOPIC_CHANGE
            Regex("""\b(ow|ouch|scared|sad|hurt|worried|triste|peur)\b""").containsMatchIn(clean) ->
                InterruptionKind.EMOTIONAL
            clean.length < 3 -> InterruptionKind.UNCLEAR
            else -> InterruptionKind.OTHER
        }
    }
}
