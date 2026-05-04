package org.evomaster.core.problem.asyncapi.schema

import com.fasterxml.jackson.databind.JsonNode

/**
 * AsyncAPI 3.0 component-level message definition (i.e. under `components.messages.<id>`).
 */
data class AsyncAPIMessage(
    /** Component id (map key under `components.messages:`). */
    val id: String,
    /** Optional explicit `name:` field; fallback to [id] when absent. */
    val name: String,
    /** MIME type carried, falls back to the schema's defaultContentType. */
    val contentType: String,
    /**
     * Correlation id location as written in the schema, e.g.
     * `"$message.header#/evm-correlation-id"`. Null when the message
     * declares no correlationId.
     */
    val correlationLocation: String?,
    /**
     * Reference to the payload schema if expressed as `$ref`,
     * stripped to the component schema id (e.g. `RequestPayload`).
     * Null when the payload is inlined; in that case [payloadInline]
     * carries the raw JsonNode tree.
     */
    val payloadSchemaRef: String?,
    /** Inline payload schema, set only when no `$ref` was used. */
    val payloadInline: JsonNode?
)
