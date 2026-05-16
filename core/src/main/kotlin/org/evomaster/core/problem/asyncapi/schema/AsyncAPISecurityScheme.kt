package org.evomaster.core.problem.asyncapi.schema

/**
 * Parsed representation of an AsyncAPI 3.0 Security Scheme Object.
 *
 * The shape mirrors the spec
 * (https://www.asyncapi.com/docs/reference/specification/v3.0.0#securitySchemeObject)
 * plus enough metadata for callers to pick a matching broker-side
 * [org.evomaster.core.problem.asyncapi.broker.AsyncAPIBrokerAuthInfo].
 *
 * AsyncAPI's catalogue of scheme types includes `userPassword` (SASL/PLAIN-like),
 * `X509` (TLS client cert), `apiKey`, `httpApiKey`, `http`, `oauth2`,
 * `openIdConnect`, `plain` (Kafka SASL/PLAIN), `scramSha256`, `scramSha512`,
 * `gssapi`, `asymmetricEncryption`, `symmetricEncryption`.
 *
 * For the M9 starter slice we surface the type as a free-form string so the
 * parser never rejects a scheme it cannot natively map; whether the broker
 * client can actually use it is the broker layer's call.
 */
data class AsyncAPISecurityScheme(
    /** Component key (e.g. `kafkaScram`). */
    val name: String,
    /** Spec-declared `type` field, lowercased (e.g. `scramsha256`, `plain`, `x509`). */
    val type: String,
    /** For `apiKey` schemes: where the key lives (`header` / `query` / `user` / `password`). */
    val `in`: String? = null,
    /** For `http` schemes: `basic` / `bearer` / etc. */
    val scheme: String? = null,
    /** For `http` schemes with bearer format. */
    val bearerFormat: String? = null,
    /** Human-readable description (kept for downstream tooling). */
    val description: String? = null
)
