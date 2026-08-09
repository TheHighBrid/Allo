package com.kenza.callsim.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ElevenLabsAuthTest {

    @Test
    fun blankKeyUsesPublicAgentPath() {
        assertEquals(
            ElevenLabsAuth.ApiKeyKind.EMPTY,
            ElevenLabsAuth.classifyApiKey("   ")
        )
    }

    @Test
    fun skKeyUsesSignedUrlPath() {
        assertEquals(
            ElevenLabsAuth.ApiKeyKind.SECRET,
            ElevenLabsAuth.classifyApiKey(" sk_example_secret ")
        )
    }

    @Test
    fun dashboardKeyIdIsNeverTreatedAsSecret() {
        assertEquals(
            ElevenLabsAuth.ApiKeyKind.KEY_ID_OR_INVALID,
            ElevenLabsAuth.classifyApiKey("a1b2c3d4e5f6")
        )
    }

    @Test
    fun detectsElevenLabsKeyIdErrorPayload() {
        val message = "invalid_api_key: API key ID used as API key - only valid API keys can be used"
        assertTrue(ElevenLabsAuth.isInvalidApiKeyFailure(message))
    }

    @Test
    fun authorizationFailureRecognizesPrivateAgentClose() {
        assertTrue(ElevenLabsAuth.isLikelyAuthorizationFailure(1008, "authorization required"))
        assertTrue(ElevenLabsAuth.isLikelyAuthorizationFailure(403, "HTTP 403"))
    }

    @Test
    fun invalidAgentDoesNotGetMisreportedAsKeyProblem() {
        assertFalse(ElevenLabsAuth.isLikelyAuthorizationFailure(1008, "invalid agent id"))
    }
}
