package org.evomaster.core.problem.rest.arazzo.parser

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import org.evomaster.core.problem.rest.arazzo.models.ArazzoSpecifications
import com.fasterxml.jackson.module.kotlin.readValue
import org.evomaster.core.problem.rest.arazzo.mapper.ArazzoMapper
import org.evomaster.core.problem.rest.arazzo.models.Workflow
import org.evomaster.core.problem.rest.arazzo.models.raws.ArazzoSpecificationsRaw
import org.evomaster.core.problem.rest.arazzo.resolver.ArazzoReferenceResolver
import org.evomaster.core.problem.rest.schema.SchemaArazzo
import org.evomaster.core.problem.rest.schema.SchemaOpenAPI

object ArazzoParser {

    val jsonMapper = ObjectMapper().findAndRegisterModules()
    val yamlMapper = ObjectMapper(YAMLFactory()).findAndRegisterModules()

    fun parse(schemaText: String, schemaOpenAPI: SchemaOpenAPI) : ArazzoSpecifications {
        val (raw, schemaJsonNode) = parseSchemaText(schemaText)
        val resolver = ArazzoReferenceResolver(raw.components,schemaJsonNode,schemaOpenAPI.schemaParsed)
        val mapper = ArazzoMapper(resolver)
        return mapper.toDomain(raw)
    }

    private fun parseSchemaText(schemaText: String): Pair<ArazzoSpecificationsRaw, JsonNode> {
        val schemaTextClean = schemaText.trimStart()

        var arazzoJsonNode: JsonNode?
        var arazzoSpecificationsRaw: ArazzoSpecificationsRaw?

        try {
            if (schemaTextClean.startsWith("{")) {
                arazzoSpecificationsRaw = jsonMapper.readValue<ArazzoSpecificationsRaw>(schemaTextClean)
                arazzoJsonNode = jsonMapper.readTree(schemaTextClean)
            } else {
                arazzoSpecificationsRaw = yamlMapper.readValue<ArazzoSpecificationsRaw>(schemaTextClean)
                arazzoJsonNode = yamlMapper.readTree(schemaTextClean)
            }
        } catch (e: Exception) {
            throw IllegalArgumentException("Problems parsing the Arazzo document", e)
        }

        return Pair(arazzoSpecificationsRaw, arazzoJsonNode)

    }

}