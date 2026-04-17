package org.evomaster.core.problem.rest.schema

import com.fasterxml.jackson.databind.JsonNode
import io.swagger.v3.oas.models.OpenAPI
import org.evomaster.core.problem.rest.arazzo.models.ArazzoSpecifications

class SchemaArazzo(
    /**
     * The actual raw value of the schema file, as a string
     */
    val schemaRaw: String,
    /**
     * A parsed schema
     */
    val schemaParsed: ArazzoSpecifications,
    /**
     * A parsed schema
     */
    val schemaJsonNode: JsonNode,
    /**
     * information about the location the schema was retrieved from, e.g.,
     * from file, URL or in memory in our tests.
     */
    val sourceLocation: SchemaLocation
) {
}