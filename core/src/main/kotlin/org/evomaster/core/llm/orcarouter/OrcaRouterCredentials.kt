package org.evomaster.core.llm.orcarouter

/**
 * API-key handling for the OrcaRouter provider.
 *
 * EvoMaster has no credential store of its own: LLM credentials are supplied through the
 * existing configuration mechanism (`--llmApiKey` / config file) and, as a convenience for
 * CI and shell use, through environment variables. Nothing here persists or refreshes a key;
 * OrcaRouter keys are durable until the user revokes them in the OrcaRouter console.
 */
object OrcaRouterCredentials {

    const val KEY_PREFIX = "sk-orca-"

    /** Environment variables checked, in order, when `--llmApiKey` is not set. */
    val ENV_VARS = listOf("ORCA_API_KEY", "ORCAROUTER_API_KEY")

    /**
     * Resolves the key to use: explicit configuration first, then the documented
     * environment variables. An empty/blank value is treated as "not configured".
     */
    fun resolve(
        configuredKey: String?,
        env: (String) -> String? = { System.getenv(it) }
    ): String? {
        configuredKey?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
        for (name in ENV_VARS) {
            val value = env(name)?.trim()
            if (!value.isNullOrEmpty()) {
                return value
            }
        }
        return null
    }

    /**
     * Lightweight shape check only. An `sk-orca-…` prefix is not proof the key is valid;
     * validity is established by the first real request.
     */
    fun hasExpectedShape(key: String?): Boolean =
        key != null && key.trim().startsWith(KEY_PREFIX) && key.trim().length > KEY_PREFIX.length

    fun requireValid(key: String?): String {
        val resolved = key?.trim()
        if (resolved.isNullOrEmpty()) {
            throw IllegalArgumentException(
                "OrcaRouter requires an API key: pass --llmApiKey <sk-orca-…>, " +
                        "set ORCA_API_KEY, or create a key at ${OrcaRouterOrigins.PUBLIC_AUTH_ORIGIN}/console"
            )
        }
        if (!hasExpectedShape(resolved)) {
            throw IllegalArgumentException(
                "The OrcaRouter API key does not have the expected '${KEY_PREFIX}' prefix"
            )
        }
        return resolved
    }

    /**
     * Redacted representation safe for logs and error messages.
     */
    fun redact(key: String?): String {
        val value = key?.trim()
        if (value.isNullOrEmpty()) {
            return "<not set>"
        }
        val tail = if (value.length >= 4) value.takeLast(4) else "****"
        return "$KEY_PREFIX****$tail"
    }

    /**
     * Maps an authentication rejection to an actionable message. OrcaRouter keys are durable
     * grants: there is no refresh endpoint and no refresh token, so a rejected key can only be
     * replaced by the user.
     */
    fun authenticationFailureHint(providerMessage: String?): String {
        val hint = "OrcaRouter rejected the credential. Replace it with a valid key from " +
                "${OrcaRouterOrigins.PUBLIC_AUTH_ORIGIN}/console (or run with --llmApiKey). " +
                "Revoked keys cannot be refreshed."
        return if (providerMessage.isNullOrBlank()) hint else "$hint Cause: $providerMessage"
    }

    /**
     * True when the failure looks like an authentication rejection (HTTP 401) rather than a
     * transient network problem. Used only to choose the error text; never to trigger retries.
     */
    fun isAuthenticationFailure(throwable: Throwable?): Boolean {
        var current = throwable
        var guard = 0
        while (current != null && guard < 10) {
            val message = current.message ?: ""
            // A per-model scope rejection is not a revoked credential: the key still works for
            // other models, so it must not be reported as a reauthentication requirement.
            if (message.contains(MODEL_ACCESS_DENIED_CODE)) {
                return false
            }
            if (message.contains("401") ||
                message.contains("Unauthorized", ignoreCase = true) ||
                message.contains("invalid_api_key")
            ) {
                return true
            }
            if (current.javaClass.simpleName == "AuthenticationException") {
                return true
            }
            current = current.cause
            guard++
        }
        return false
    }

    /**
     * True when the provider rejected the request because the key/workspace cannot use the
     * selected model. The credential itself is still valid.
     */
    fun isModelAccessDenied(throwable: Throwable?): Boolean {
        var current = throwable
        var guard = 0
        while (current != null && guard < 10) {
            if (current.message?.contains(MODEL_ACCESS_DENIED_CODE) == true) {
                return true
            }
            current = current.cause
            guard++
        }
        return false
    }

    const val MODEL_ACCESS_DENIED_CODE = "model_access_denied"
}
