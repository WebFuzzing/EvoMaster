package org.evomaster.core.problem.asyncapi.builder

import com.fasterxml.jackson.databind.JsonNode
import io.swagger.v3.oas.models.media.Schema
import org.evomaster.core.problem.asyncapi.schema.JsonSchemaConverter
import org.evomaster.core.search.gene.builder.SchemaRefResolver

/**
 * AsyncAPI-side adapter from [SchemaRefResolver] to a flat
 * `Map<String, JsonNode>` of component schemas.
 *
 * AsyncAPI 3.0 component schemas are plain JSON Schema, parsed lazily —
 * we keep them as raw [JsonNode] trees in [AsyncAPISchema] and convert
 * each one to a Swagger [Schema] on demand here so the shared
 * [org.evomaster.core.search.gene.builder.JsonSchemaToGeneConverter] can
 * consume the same `Schema<*>` shape REST already produces.
 *
 * `$ref` strings come in OpenAPI form (`"#/components/schemas/<id>"`); the
 * trailing segment after the last `/` is treated as the component id.
 */
class AsyncAPISchemaRefResolver(
    private val componentSchemas: Map<String, JsonNode>
) : SchemaRefResolver {

    override fun resolve(ref: String, messages: MutableList<String>): Schema<*>? {
        val id = ref.substringAfterLast('/')
        val node = componentSchemas[id]
        if (node == null) {
            messages.add("AsyncAPI component schema '$id' not found for reference '$ref'")
            return null
        }
        return JsonSchemaConverter.convert(node)
    }
}
