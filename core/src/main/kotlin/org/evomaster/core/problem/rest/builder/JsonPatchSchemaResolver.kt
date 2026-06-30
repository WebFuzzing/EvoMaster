package org.evomaster.core.problem.rest.builder

import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.Schema
import org.evomaster.core.problem.rest.schema.RestSchema
import org.evomaster.core.problem.rest.schema.SchemaOpenAPI
import org.evomaster.core.problem.rest.schema.SchemaUtils

/**
 * Resolves the target resource schema for a JSON Patch endpoint by inspecting
 * sibling operations on the same path item.
 *
 * Priority: GET 2xx response → PUT requestBody → POST requestBody.
 */
object JsonPatchSchemaResolver {

    private const val JSON_PATCH_MEDIA_TYPE = "json-patch"

    fun resolveResourceSchema(
        operation: Operation,
        schemaHolder: RestSchema,
        currentSchema: SchemaOpenAPI,
        messages: MutableList<String>
    ): Schema<*>? {
        val pathItem = findPathItemForPatchOperation(operation, schemaHolder, currentSchema, messages)
            ?: return null

        return fromGetResponse(pathItem, schemaHolder, currentSchema, messages)
            ?: fromRequestBody(pathItem.put, schemaHolder, currentSchema, messages)
            ?: fromRequestBody(pathItem.post, schemaHolder, currentSchema, messages)
    }

    //handleBodyPayload does not have the path of the operation, we need to find it, only if it is patch
    private fun findPathItemForPatchOperation(
        operation: Operation,
        schemaHolder: RestSchema,
        currentSchema: SchemaOpenAPI,
        messages: MutableList<String>
    ): PathItem? {
        return schemaHolder.main.schemaParsed.paths
            ?.values
            ?.firstNotNullOfOrNull { pathItemOrRef ->
                val pathItem = if (pathItemOrRef.`$ref` != null) {
                    SchemaUtils.getReferencePathItem(schemaHolder, currentSchema, pathItemOrRef.`$ref`, messages)
                } else {
                    pathItemOrRef
                }

                pathItem?.takeIf { it.patch === operation }
            }
    }

    // For get, the resource is in the response
    private fun fromGetResponse(
        pathItem: PathItem,
        schemaHolder: RestSchema,
        currentSchema: SchemaOpenAPI,
        messages: MutableList<String>
    ): Schema<*>? {
        val get = pathItem.get ?: return null
        return get.responses
            ?.filter { (code, _) -> code.startsWith("2") }
            ?.values
            ?.firstNotNullOfOrNull { response ->
                val resolved = if (response.`$ref` != null) {
                    SchemaUtils.getReferenceResponse(schemaHolder, currentSchema, response.`$ref`, messages)
                        ?: return@firstNotNullOfOrNull null
                } else response
                extractJsonSchema(resolved.content, schemaHolder, currentSchema, messages)
            }
    }

    // For put and post, the resource is in the requestBody
    private fun fromRequestBody(
        operation: Operation?,
        schemaHolder: RestSchema,
        currentSchema: SchemaOpenAPI,
        messages: MutableList<String>
    ): Schema<*>? {
        val body = operation?.requestBody ?: return null
        val resolvedBody = if (body.`$ref` != null) {
            SchemaUtils.getReferenceRequestBody(schemaHolder, currentSchema, body.`$ref`, messages)
                ?: return null
        } else body
        return extractJsonSchema(resolvedBody.content, schemaHolder, currentSchema, messages)
    }

    private fun extractJsonSchema(
        content: Map<String, MediaType>?,
        schemaHolder: RestSchema,
        currentSchema: SchemaOpenAPI,
        messages: MutableList<String>
    ): Schema<*>? {
        val schema = content
            ?.filterKeys { mt -> mt.contains("json") && !mt.contains(JSON_PATCH_MEDIA_TYPE) }
            ?.values
            ?.firstOrNull()
            ?.schema
            ?: return null
        return if (schema.`$ref` != null) {
            SchemaUtils.getReferenceSchema(schemaHolder, currentSchema, schema.`$ref`, messages)
        } else schema
    }
}
