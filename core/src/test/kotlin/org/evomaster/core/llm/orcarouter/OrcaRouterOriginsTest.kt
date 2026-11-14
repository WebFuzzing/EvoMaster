package org.evomaster.core.llm.orcarouter

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class OrcaRouterOriginsTest {

    private fun env(vararg pairs: Pair<String, String>): (String) -> String? {
        val map = pairs.toMap()
        return { name -> map[name] }
    }

    @Test
    fun testDefaultApiBaseIsPublicInferenceOrigin() {
        assertEquals(
            "https://api.orcarouter.ai/v1",
            OrcaRouterOrigins.resolveApiBase(env = env())
        )
        // The auth origin is never derived from the inference origin.
        assertFalse(OrcaRouterOrigins.resolveApiBase(env = env()).contains("www.orcarouter.ai"))
    }

    @Test
    fun testExplicitOverrideTakesPrecedence() {
        assertEquals(
            "https://relay.internal.example/v1",
            OrcaRouterOrigins.resolveApiBase(
                explicit = "https://relay.internal.example/v1",
                env = env(
                    OrcaRouterOrigins.ENV_API_BASE_URL to "https://api-env.example/v1",
                    OrcaRouterOrigins.ENV_SHARED_BASE_URL to "https://shared.example"
                )
            )
        )
    }

    @Test
    fun testApiBaseEnvBeatsSharedBase() {
        assertEquals(
            "https://api-env.example/v1",
            OrcaRouterOrigins.resolveApiBase(
                env = env(
                    OrcaRouterOrigins.ENV_API_BASE_URL to "https://api-env.example/v1",
                    OrcaRouterOrigins.ENV_SHARED_BASE_URL to "https://shared.example"
                )
            )
        )
    }

    @Test
    fun testSharedBaseGetsApiSuffixOnce() {
        assertEquals(
            "https://shared.example/v1",
            OrcaRouterOrigins.resolveApiBase(
                env = env(OrcaRouterOrigins.ENV_SHARED_BASE_URL to "https://shared.example/")
            )
        )
        assertEquals(
            "https://shared.example/v1",
            OrcaRouterOrigins.resolveApiBase(
                env = env(OrcaRouterOrigins.ENV_SHARED_BASE_URL to "https://shared.example/v1")
            )
        )
    }

    @Test
    fun testRemoteOriginsRequireHttps() {
        assertThrows(IllegalArgumentException::class.java) {
            OrcaRouterOrigins.resolveApiBase(explicit = "http://api.orcarouter.ai/v1", env = env())
        }
        assertThrows(IllegalArgumentException::class.java) {
            OrcaRouterOrigins.resolveApiBase(explicit = "ftp://api.orcarouter.ai/v1", env = env())
        }
    }

    @Test
    fun testLoopbackHttpIsAllowedForDevelopment() {
        assertEquals(
            "http://127.0.0.1:8080/v1",
            OrcaRouterOrigins.resolveApiBase(explicit = "http://127.0.0.1:8080/v1", env = env())
        )
        assertEquals(
            "http://localhost:8080/v1",
            OrcaRouterOrigins.resolveApiBase(explicit = "http://localhost:8080/v1/", env = env())
        )
    }

    @Test
    fun testUserInfoAndFragmentAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            OrcaRouterOrigins.resolveApiBase(explicit = "https://user:pass@api.orcarouter.ai/v1", env = env())
        }
        assertThrows(IllegalArgumentException::class.java) {
            OrcaRouterOrigins.resolveApiBase(explicit = "https://api.orcarouter.ai/v1#frag", env = env())
        }
    }

    @Test
    fun testCredentialsResolvePrecedenceAndClear() {
        val envFn = env(
            "ORCA_API_KEY" to "sk-orca-from-orca-env",
            "ORCAROUTER_API_KEY" to "sk-orca-from-absolute-env"
        )
        assertEquals("sk-orca-from-config", OrcaRouterCredentials.resolve(" sk-orca-from-config ", envFn))
        assertEquals("sk-orca-from-orca-env", OrcaRouterCredentials.resolve(null, envFn))
        assertEquals("sk-orca-from-orca-env", OrcaRouterCredentials.resolve("   ", envFn))

        // "cleared": no configured value and no environment value
        assertNull(OrcaRouterCredentials.resolve(null, env()))

        val onlySecond = env("ORCAROUTER_API_KEY" to "sk-orca-second")
        assertEquals("sk-orca-second", OrcaRouterCredentials.resolve(null, onlySecond))
    }

    @Test
    fun testKeyShapeValidationAndRedaction() {
        assertTrue(OrcaRouterCredentials.hasExpectedShape("sk-orca-abc123"))
        assertFalse(OrcaRouterCredentials.hasExpectedShape("sk-other-abc123"))
        assertFalse(OrcaRouterCredentials.hasExpectedShape("sk-orca-"))
        assertFalse(OrcaRouterCredentials.hasExpectedShape(null))

        val secret = "sk-orca-super-secret-value-9876"
        val redacted = OrcaRouterCredentials.redact(secret)
        assertFalse(redacted.contains("super-secret"))
        assertTrue(redacted.startsWith(OrcaRouterCredentials.KEY_PREFIX))
        assertTrue(redacted.endsWith("9876"))
        assertEquals("<not set>", OrcaRouterCredentials.redact(null))
    }

    @Test
    fun testInvalidKeyErrorsDoNotContainTheKey() {
        val secret = "totally-wrong-value-1234"
        val error = assertThrows(IllegalArgumentException::class.java) {
            OrcaRouterCredentials.requireValid(secret)
        }
        assertFalse(error.message!!.contains(secret))
        assertTrue(error.message!!.contains(OrcaRouterCredentials.KEY_PREFIX))
    }

    @Test
    fun testAuthenticationFailureClassificationAndHint() {
        assertTrue(OrcaRouterCredentials.isAuthenticationFailure(IllegalStateException("Request failed with 401")))
        assertTrue(
            OrcaRouterCredentials.isAuthenticationFailure(
                RuntimeException("wrapper", IllegalStateException("Unauthorized"))
            )
        )
        assertFalse(OrcaRouterCredentials.isAuthenticationFailure(IllegalStateException("timeout")))
        assertFalse(OrcaRouterCredentials.isAuthenticationFailure(null))

        // A per-model scope rejection is not a revoked credential.
        val denied = IllegalStateException(
            "{\"error\":{\"code\":\"model_access_denied\",\"message\":\"no access to model\"}}"
        )
        assertFalse(OrcaRouterCredentials.isAuthenticationFailure(denied))
        assertTrue(OrcaRouterCredentials.isModelAccessDenied(denied))
        assertFalse(OrcaRouterCredentials.isModelAccessDenied(IllegalStateException("Request failed with 401")))

        val hint = OrcaRouterCredentials.authenticationFailureHint("HTTP 401")
        assertTrue(hint.contains("console"))
        assertFalse(hint.contains("refresh token"))
    }
}
