package com.kenza.callsim.script

/** Creates plain, shareable script text without production-only bracketed directions. */
object ScriptTextExport {

    fun clean(scriptText: String): String = scriptText
        .replace(BRACKETED_DIRECTION, "")
        .lineSequence()
        .map(String::trim)
        .fold(mutableListOf<String>()) { lines, line ->
            if (line.isNotEmpty() || lines.lastOrNull()?.isNotEmpty() == true) lines += line
            lines
        }
        .joinToString("\n")
        .trim()

    private val BRACKETED_DIRECTION = Regex("\\[[^]]*]")
}
