package org.evomaster.core.problem.rest.arazzo.parser

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import org.evomaster.core.problem.rest.arazzo.models.ArazzoSpecifications
import com.fasterxml.jackson.module.kotlin.readValue
import io.swagger.v3.oas.models.OpenAPI
import org.evomaster.core.problem.rest.schema.SchemaArazzo
import org.evomaster.core.problem.rest.schema.SchemaOpenAPI

object ArazzoParser {

    val jsonMapper = ObjectMapper().findAndRegisterModules()
    val yamlMapper = ObjectMapper(YAMLFactory()).findAndRegisterModules()

    fun parserSchemaText(schemaText: String): ArazzoSpecifications {
        val schemaTextClean = schemaText.trimStart()

        var arazzoSpecifications: ArazzoSpecifications?

        try {
            if (schemaTextClean.startsWith("{")) {
                arazzoSpecifications = jsonMapper.readValue<ArazzoSpecifications>(schemaTextClean)
            } else {
                arazzoSpecifications = yamlMapper.readValue<ArazzoSpecifications>(schemaTextClean)
            }
        } catch (e: Exception) {
            throw IllegalArgumentException("Problems parsing the Arazzo document", e)
        }

        return arazzoSpecifications

    }

    fun validateSchema(schemaArazzo: SchemaArazzo, schemaOpenAPI: SchemaOpenAPI) {

    }

}