package com.kenza.callsim.script

import com.kenza.callsim.memory.KenzaContext

/** A normalized one-sided script that can be reviewed or rendered by a later provider adapter. */
data class ScriptGeneration(
    val title: String,
    val language: String,
    val mode: ScriptMode,
    val ttsText: String,
    val duration: ScriptDurationEstimate,
    val isDemo: Boolean,
    val memoryIdsUsed: List<String> = emptyList(),
)

/** Boundary shared by offline and network-backed Script Studio generators. */
interface ScriptGenerator {
    suspend fun generate(request: ScriptRequest, context: KenzaContext): Result<ScriptGeneration>

    suspend fun generate(request: ScriptRequest): Result<ScriptGeneration> =
        generate(request, KenzaContext())
}

/**
 * Generates a short, deterministic sample without provider credentials, network access, or cost.
 * It deliberately marks the result as a demo so callers do not confuse it with live AI output.
 */
class DemoScriptGenerator : ScriptGenerator {

    override suspend fun generate(
        request: ScriptRequest,
        context: KenzaContext,
    ): Result<ScriptGeneration> {
        val validation = ScriptRequestValidator.validate(request)
        if (!validation.isValid) {
            return Result.failure(IllegalArgumentException(validation.errors.first()))
        }

        val ttsText = demoTextFor(request.mode)
        return Result.success(
            ScriptGeneration(
                title = "Demo: ${request.mode.displayName()} call",
                language = request.language.trim(),
                mode = request.mode,
                ttsText = ttsText,
                duration = ScriptDurationEstimator.estimate(ttsText),
                isDemo = true,
                memoryIdsUsed = context.memoryIdsUsed,
            ),
        )
    }

    private fun demoTextFor(mode: ScriptMode): String = when (mode) {
        ScriptMode.PLAYFUL -> """
            [soft laugh] You always say you have nothing to tell me, then somehow your day has ten little stories.
            [listening pause 4 seconds]
            No, no, keep going. I want the dramatic version, not the short version.
            [brief silence]
            Okay, that part is actually funny. I would have laughed too.
        """.trimIndent()

        ScriptMode.SUPPORTIVE, ScriptMode.AFTER_ARGUMENT -> """
            Hey. I can hear that today took a lot out of you.
            [listening pause 5 seconds]
            You do not have to make it sound smaller for me. Tell me the part that is still bothering you.
            [brief silence]
            We can take it one piece at a time, okay?
        """.trimIndent()

        else -> """
            Hey, I was hoping I would catch you for a minute.
            [listening pause 3 seconds]
            Nothing dramatic. I just wanted to hear how your day actually went.
            [brief silence]
            There you are. That already sounds more like you.
        """.trimIndent()
    }

    private fun ScriptMode.displayName(): String = name
        .lowercase()
        .split('_')
        .joinToString(" ") { word -> word.replaceFirstChar(Char::uppercase) }
}
