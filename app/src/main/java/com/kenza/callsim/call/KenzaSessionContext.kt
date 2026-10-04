package com.kenza.callsim.call

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.random.Random

/** Who placed the current call. */
internal enum class CallDirection { MOHAMED_CALLED, KENZA_CALLED }

/**
 * Kenza's first words on ElevenLabs, delivered as the agent `first_message` through the
 * `{{opening_line}}` dynamic variable. A first message is spoken by TTS without waiting for the
 * LLM, so the call starts the way a real one does.
 */
internal object KenzaCallOpening {

    /** Mohamed called her: she picks up. */
    val PICKUP_LINES = listOf("Allô?", "Allô? … hey Mousse.", "Allô, oui?")

    /** She called him: a short caller greeting; the reason for calling follows from the prompt. */
    val CALLER_LINES = listOf("Allô? Hey Mousse, it's me.", "Hey babe… you got a sec?", "Allô! Hey, t'es là?")

    /**
     * Empty once anything was already heard in this call (a reconnect or account failover): an
     * empty first message makes the agent wait instead of replaying a greeting mid-conversation.
     */
    fun openingLine(direction: CallDirection, alreadyHeardThisCall: Boolean, random: Random = Random.Default): String {
        if (alreadyHeardThisCall) return ""
        val lines = if (direction == CallDirection.KENZA_CALLED) CALLER_LINES else PICKUP_LINES
        return lines[random.nextInt(lines.size)]
    }
}

/**
 * ElevenLabs `dynamic_variables` that carry the persona and memory into the agent's own prompt
 * and first message. Unlike the full prompt override, dynamic variables need no Security toggle.
 * The agent prompt must reference these names (see docs/ELEVENLABS_AGENT.md).
 */
internal object KenzaDynamicVariables {
    const val PERSONA = "kenza_persona"
    const val CONTINUITY_BRIEFING = "continuity_briefing"
    const val CALL_DIRECTION = "call_direction"
    const val CALL_REASON = "call_reason"
    const val LOCAL_TIME = "local_time"
    const val OPENING_LINE = "opening_line"

    val ALL = listOf(PERSONA, CONTINUITY_BRIEFING, CALL_DIRECTION, CALL_REASON, LOCAL_TIME, OPENING_LINE)

    private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, h:mm a", Locale.ENGLISH)

    /** Stand-in for empty context: an empty value may be treated as a missing variable. */
    const val NONE = "none"

    /**
     * Every variable is always present so the agent never sees an unresolved {{name}}. Only
     * [OPENING_LINE] may be empty, deliberately, so a reconnect does not replay the greeting.
     */
    fun build(
        persona: String,
        continuityBriefing: String,
        callReason: String,
        direction: CallDirection,
        openingLine: String,
        now: ZonedDateTime,
    ): Map<String, String> = linkedMapOf(
        PERSONA to persona.trim().ifEmpty { NONE },
        CONTINUITY_BRIEFING to continuityBriefing.trim().ifEmpty { NONE },
        CALL_DIRECTION to describe(direction),
        CALL_REASON to callReason.trim().ifEmpty { NONE },
        LOCAL_TIME to "${TIME_FORMAT.format(now)} (${now.zone.id})",
        OPENING_LINE to openingLine,
    )

    fun describe(direction: CallDirection): String = when (direction) {
        CallDirection.MOHAMED_CALLED ->
            "Mohamed called you and you just picked up. You are the one answering the phone."
        CallDirection.KENZA_CALLED ->
            "You called Mohamed. You are the caller, so never thank him for calling."
    }
}
