package com.kenza.callsim.memory

/**
 * Compatibility facade retained for existing live-call callers.
 *
 * The actual context selection now lives in [KenzaContextAssembler] so Live Call Mode and
 * Script Studio share the same persona, memory retrieval, and continuity policy.
 */
object MemoryContext {

    fun build(store: MemoryStore, contactName: String, now: Long): String =
        KenzaContextAssembler.assemble(
            store = store,
            personaName = contactName,
            now = now,
        ).toPrompt()
}
