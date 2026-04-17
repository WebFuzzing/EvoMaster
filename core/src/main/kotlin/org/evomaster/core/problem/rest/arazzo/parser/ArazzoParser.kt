package org.evomaster.core.problem.rest.arazzo.parser

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import org.evomaster.core.problem.rest.arazzo.models.ArazzoSpecifications
import com.fasterxml.jackson.module.kotlin.readValue
import org.evomaster.core.problem.rest.arazzo.resolver.ArazzoReferenceResolver
import org.evomaster.core.problem.rest.schema.SchemaArazzo
import org.evomaster.core.problem.rest.schema.SchemaOpenAPI

object ArazzoParser {

    val jsonMapper = ObjectMapper().findAndRegisterModules()
    val yamlMapper = ObjectMapper(YAMLFactory()).findAndRegisterModules()

    fun parseSchemaText(schemaText: String): Pair<ArazzoSpecifications, JsonNode> {
        val schemaTextClean = schemaText.trimStart()

        var arazzoSpecifications: ArazzoSpecifications?
        var arazzoJsonNode: JsonNode?

        try {
            if (schemaTextClean.startsWith("{")) {
                arazzoSpecifications = jsonMapper.readValue<ArazzoSpecifications>(schemaTextClean)
                arazzoJsonNode = jsonMapper.readTree(schemaTextClean)
            } else {
                arazzoSpecifications = yamlMapper.readValue<ArazzoSpecifications>(schemaTextClean)
                arazzoJsonNode = yamlMapper.readTree(schemaTextClean)
            }
        } catch (e: Exception) {
            throw IllegalArgumentException("Problems parsing the Arazzo document", e)
        }

        return Pair(arazzoSpecifications, arazzoJsonNode)

    }

    fun validateSchema(schemaArazzo: SchemaArazzo, schemaOpenAPI: SchemaOpenAPI) {
        schemaArazzo.schemaParsed.sourceDescriptions.forEach {
            sourceDescription -> ArazzoValidator.validateSourceDescriptions(sourceDescription)
        }

        ArazzoValidator.validateComponents(schemaArazzo.schemaParsed.components)

        val resolver = ArazzoReferenceResolver(
            schemaArazzo.schemaParsed.components,
            schemaArazzo.schemaJsonNode,
            schemaOpenAPI.schemaParsed
        )
        ArazzoValidator.configResolver(resolver)

        ArazzoValidator.validateWorkflows(schemaArazzo.schemaParsed.workflows)
    }

}