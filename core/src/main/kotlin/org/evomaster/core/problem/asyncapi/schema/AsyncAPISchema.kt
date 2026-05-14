package org.evomaster.core.problem.asyncapi.schema

import com.fasterxml.jackson.databind.JsonNode
import org.evomaster.core.problem.rest.schema.SchemaLocation

/**
 * Parsed AsyncAPI 3.0 document, normalised so EvoMaster's downstream code
 * does not have to walk the raw YAML/JSON tree.
 *
 * Component message payload schemas remain as raw [JsonNode] trees here; the
 * conversion to swagger media [io.swagger.v3.oas.models.media.Schema] happens in
 * the AsyncAPI action-builder layer (see plan M4) so this module stays free of
 * gene/builder dependencies.
 */
data class AsyncAPISchema(
    val rawText: String,
    val location: SchemaLocation,
    /** AsyncAPI version string from the document, e.g. "3.0.0". */
    val version: String,
    /** Channel key → channel. */
    val channels: Map<String, AsyncAPIChannel>,
    /** Operation key → operation. */
    val operations: Map<String, AsyncAPIOperation>,
    /** Component message id → message. */
    val messages: Map<String, AsyncAPIMessage>,
    /** Component schema id → raw JSON Schema node. */
    val componentSchemas: Map<String, JsonNode>,
    /** Default contentType declared at top level (or "application/json"). */
    val defaultContentType: String,
    /** Server entries keyed by server name. */
    val servers: Map<String, AsyncAPIServer>,
    /** `components.securitySchemes` (key → scheme). Empty when none declared. */
    val securitySchemes: Map<String, AsyncAPISecurityScheme> = emptyMap()
)

data class AsyncAPIServer(
    val name: String,
    val host: String,
    val protocol: String
)
