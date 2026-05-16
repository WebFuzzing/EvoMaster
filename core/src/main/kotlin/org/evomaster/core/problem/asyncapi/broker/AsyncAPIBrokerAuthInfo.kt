package org.evomaster.core.problem.asyncapi.broker

/**
 * Connection-level broker auth for AsyncAPI black-box runs.
 *
 * Kafka authenticates at the *connection*, not per-message, so a single
 * instance is threaded through every produce / consume call. White-box mode
 * may later override this from a driver-supplied `SutInfoDto.asyncAPIProblem.brokerAuth`;
 * for now black-box reads CLI flags via `EMConfig.toBrokerAuth()`.
 */
sealed class AsyncAPIBrokerAuthInfo {

    object NoAuth : AsyncAPIBrokerAuthInfo()

    /**
     * SASL/PLAIN: username + password sent in cleartext on the connection.
     * Requires TLS in production; permitted as plaintext for local CI.
     */
    data class SaslPlain(
        val username: String,
        val password: String,
        /** When true, SASL is wrapped in TLS (`SASL_SSL`); otherwise `SASL_PLAINTEXT`. */
        val tls: Boolean = false
    ) : AsyncAPIBrokerAuthInfo()

    /**
     * SASL/SCRAM-SHA-256: challenge-response with hashed password storage on
     * the broker side. Same `tls` toggle as [SaslPlain].
     */
    data class SaslScramSha256(
        val username: String,
        val password: String,
        val tls: Boolean = false
    ) : AsyncAPIBrokerAuthInfo()

    /**
     * Plain TLS (no SASL). Optionally provides client cert material; a
     * server-only TLS handshake works with both paths null.
     */
    data class Ssl(
        val truststorePath: String? = null,
        val truststorePassword: String? = null,
        val keystorePath: String? = null,
        val keystorePassword: String? = null
    ) : AsyncAPIBrokerAuthInfo()
}
