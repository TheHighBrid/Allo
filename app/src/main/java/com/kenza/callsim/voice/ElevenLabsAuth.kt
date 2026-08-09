package com.kenza.callsim.voice

/**
 * Small, dependency-free policy for ElevenLabs credential handling.
 *
 * ElevenLabs now distinguishes the secret API key value from its dashboard key ID.
 * Secret keys are the values that can authenticate API requests and currently begin
 * with `sk_`. A key ID must never be sent in the `xi-api-key` header.
 */
internal object ElevenLabsAuth {

    enum class ApiKeyKind { EMPTY, SECRET, KEY_ID_OR_INVALID }

    fun classifyApiKey(raw: String): ApiKeyKind {
        val key = raw.trim()
        return when {
            key.isEmpty() -> ApiKeyKind.EMPTY
            key.startsWith("sk_") -> ApiKeyKind.SECRET
            else -> ApiKeyKind.KEY_ID_OR_INVALID
        }
    }

    fun isInvalidApiKeyFailure(message: String?): Boolean {
        val m = message.orEmpty().lowercase()
        return m.contains("invalid_api_key") ||
            m.contains("api key id used as api key") ||
            m.contains("only valid api keys can be used")
    }

    fun isLikelyAuthorizationFailure(code: Int, reason: String?): Boolean {
        val r = reason.orEmpty().lowercase()
        if (r.contains("agent") && (r.contains("not found") || r.contains("invalid"))) return false
        return code == 401 || code == 403 || code == 1008 ||
            r.contains("unauthorized") ||
            r.contains("authorization") ||
            r.contains("authentication") ||
            r.contains("requires auth") ||
            r.contains("private agent")
    }

    const val SECRET_KEY_HINT =
        "ElevenLabs needs the secret API key value for a private agent. Paste the key shown when it was created or rotated; it starts with sk_. Do not paste the Key ID. Public agents should leave the API key field blank."
}
