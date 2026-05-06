package org.evomaster.core.problem.rest.builder

import io.swagger.v3.oas.models.media.Schema
import org.evomaster.core.problem.rest.schema.RestSchema
import org.evomaster.core.problem.rest.schema.SchemaOpenAPI
import org.evomaster.core.problem.rest.schema.SchemaUtils
import org.evomaster.core.search.gene.builder.SchemaRefResolver

/**
 * REST-side adapter from [SchemaRefResolver] to the OpenAPI document model.
 *
 * Wraps the parsed [RestSchema] (the catalogue of all reachable OpenAPI
 * documents) and the [SchemaOpenAPI] currently being processed, then
 * delegates to [SchemaUtils.getReferenceSchema] for the actual lookup.
 *
 * Existence rationale: keeps REST-specific schema types out of the new
 * [org.evomaster.core.search.gene.builder.JsonSchemaToGeneConverter] so
 * AsyncAPI (and any future protocol) can use the same converter behind a
 * different resolver.
 */
class RestSchemaRefResolver(
    private val schemaHolder: RestSchema,
    private val currentSchema: SchemaOpenAPI
) : SchemaRefResolver {

    override fun resolve(ref: String, messages: MutableList<String>): Schema<*>? {
        return SchemaUtils.getReferenceSchema(schemaHolder, currentSchema, ref, messages)
    }
}
