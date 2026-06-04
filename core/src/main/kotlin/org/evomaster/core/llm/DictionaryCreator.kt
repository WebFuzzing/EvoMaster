package org.evomaster.core.llm

import org.evomaster.core.problem.rest.schema.OpenApiAccess
import java.io.File
import kotlin.io.path.Path
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.parameters.Parameter
import io.swagger.v3.oas.models.parameters.RequestBody
import org.evomaster.core.docs.ConfigToMarkdown.saveToDocs
import org.evomaster.core.docs.ConfigToMarkdown.toMarkdown


object DictionaryCreator {

    private data class FieldInfo(
        val name: String,
        val description: String?
    )

    @JvmStatic
    fun main(args: Array<String>) {
        createNameDescriptions()
    }

    fun createNameDescriptions() : Map<String, Set<String>> {

        val data = mutableMapOf<String, MutableSet<String>>()

        val locations = scanForSchemas("./src/test/resources/swagger")
            //TODO plus "../core-tests/integration-tests/core-it/src/test/resources/APIs_guru"

        for(l in locations){
            println("Analyzing: $l")
            val schema = try{
                OpenApiAccess.getOpenAPIFromLocation(l)
            } catch (e: Exception){
                println("Failed to analyze: $l")
                continue
            }

            val params = extractStringFieldInfo(schema.schemaParsed)
            for(p in params){
                val descriptions = data.getOrPut(p.name) { mutableSetOf() }
                if(p.description != null){
                    descriptions.add(p.description)
                }
            }
        }

        println("Obtained ${data.size} field info")
        return data
    }

    private fun scanForSchemas(relativePath: String) : List<String>{
        val target = File(relativePath)
        if (!target.exists()) {
            throw IllegalStateException("OpenAPI resource folder does not exist: ${target.absolutePath}")
        }

        return target.walk()
            .filter { it.isFile }
            .map {
                val s = Path(it.absolutePath).toAbsolutePath().normalize().toString()
                s
            }.toList()
    }


    private fun extractStringFieldInfo(openAPI: OpenAPI): List<FieldInfo> {
        val result = mutableListOf<FieldInfo>()

        // Process schemas/components
        openAPI.components?.schemas?.forEach { (schemaName, schema) ->
            extractStringFieldsFromSchema(schema, schemaName, result)
        }

        // Process paths (parameters and request bodies)
        openAPI.paths?.forEach { (path, pathItem) ->
            pathItem.readOperations().forEach { operation ->
                operation.parameters?.forEach { parameter ->
                    if (isStringParameter(parameter)) {
                        result.add(FieldInfo(
                            name = parameter.name ?: "unknown",
                            description = parameter.description
                        ))
                    }
                }

                // Request body
                operation.requestBody?.let { requestBody ->
                    extractStringFieldsFromRequestBody(requestBody, result)
                }
            }
        }

        return result
    }

    private fun extractStringFieldsFromSchema(
        schema: Schema<*>,
        fieldName: String,
        result: MutableList<FieldInfo>
    ) {
        if (isStringSchema(schema)) {
            result.add(FieldInfo(
                name = fieldName,
                description = schema.description
            ))
        }

        // Recursively process properties
        schema.properties?.forEach { (propName, propSchema) ->
            if (isStringSchema(propSchema)) {
                result.add(FieldInfo(
                    name = propName,
                    description = propSchema.description
                ))
            }

            if (propSchema.properties != null) {
                extractStringFieldsFromSchema(propSchema, propName, result)
            }

            if (propSchema.type == "array" && propSchema.items != null) {
                if (isStringSchema(propSchema.items)) {
                    result.add(FieldInfo(
                        name = propName,
                        description = propSchema.items.description
                    ))
                }
            }
        }
    }

    private fun extractStringFieldsFromRequestBody(
        requestBody: RequestBody,
        result: MutableList<FieldInfo>
    ) {
        requestBody.content?.forEach { (_, mediaType) ->
            mediaType.schema?.let { schema ->
                extractStringFieldsFromSchema(schema, "body", result)
            }
        }
    }

    private fun isStringParameter(parameter: Parameter): Boolean {
        return when {
            parameter.schema != null && isStringSchema(parameter.schema) -> true

            parameter.content != null -> {
                parameter.content.any { (_, mediaType) ->
                    mediaType.schema != null && isStringSchema(mediaType.schema)
                }
            }

            else -> false
        }
    }

    private fun isStringSchema(schema: Schema<*>): Boolean {
        return schema.type == "string" ||
                schema.format == "string" ||
                (schema.type == null && schema.properties == null && schema.items == null &&
                        schema.`$ref` == null)
    }
}