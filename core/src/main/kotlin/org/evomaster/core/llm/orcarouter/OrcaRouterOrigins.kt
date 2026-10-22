package org.evomaster.core.llm.orcarouter

import java.net.URI
import java.net.URISyntaxException

/**
 * Resolution of the OrcaRouter inference origin.
 *
 * Authentication endpoints live on a different public origin (www.orcarouter.ai) and are
 * intentionally not derived here: this tool only implements the API-key integration, so it
 * never talks to the auth origin. The inference base is configurable per installation:
 *
 *  1. explicit value (EvoMaster's own `--llmURL` option)
 *  2. `ORCA_API_BASE_URL`
 *  3. `ORCA_BASE_URL`, a shared self-hosted origin, with the `/v1` API suffix appended
 *  4. the public default `https://api.orcarouter.ai/v1`
 */
object OrcaRouterOrigins {

    const val DEFAULT_API_BASE_URL = "https://api.orcarouter.ai/v1"
    const val PUBLIC_AUTH_ORIGIN = "https://www.orcarouter.ai"

    const val ENV_API_BASE_URL = "ORCA_API_BASE_URL"
    const val ENV_SHARED_BASE_URL = "ORCA_BASE_URL"

    fun resolveApiBase(
        explicit: String? = null,
        env: (String) -> String? = { System.getenv(it) }
    ): String {
        explicit?.takeIf { it.isNotBlank() }?.let { return normalize(it) }
        env(ENV_API_BASE_URL)?.takeIf { it.isNotBlank() }?.let { return normalize(it) }
        env(ENV_SHARED_BASE_URL)?.takeIf { it.isNotBlank() }?.let { return normalize(withApiSuffix(it)) }
        return DEFAULT_API_BASE_URL
    }

    /**
     * Appends the `/v1` API suffix to a self-hosted shared origin, unless it is already there.
     */
    fun withApiSuffix(base: String): String {
        val trimmed = base.trim().trimEnd('/')
        return if (trimmed.endsWith("/v1")) trimmed else "$trimmed/v1"
    }

    /**
     * Normalizes and validates an inference base URL.
     * Remote origins must be HTTPS; plain HTTP is allowed only for loopback development.
     */
    fun normalize(url: String, fromSharedBase: Boolean = false): String {
        val raw = if (fromSharedBase) withApiSuffix(url) else url.trim().trimEnd('/')
        val uri = try {
            URI(raw)
        } catch (e: URISyntaxException) {
            throw IllegalArgumentException("Invalid OrcaRouter base URL")
        }
        if (uri.userInfo != null) {
            throw IllegalArgumentException("OrcaRouter base URL must not contain userinfo")
        }
        if (uri.fragment != null) {
            throw IllegalArgumentException("OrcaRouter base URL must not contain a fragment")
        }
        val scheme = uri.scheme?.lowercase()
        val host = uri.host?.lowercase()
            ?: throw IllegalArgumentException("OrcaRouter base URL must contain a host")

        if (scheme == "http") {
            if (!isLoopback(host)) {
                throw IllegalArgumentException("OrcaRouter base URL must use HTTPS for non-loopback hosts")
            }
        } else if (scheme != "https") {
            throw IllegalArgumentException("OrcaRouter base URL must use HTTPS")
        }
        return uri.toString().trimEnd('/')
    }

    fun isLoopback(host: String): Boolean =
        host == "localhost" || host == "127.0.0.1" || host == "[::1]" || host == "::1"
}
