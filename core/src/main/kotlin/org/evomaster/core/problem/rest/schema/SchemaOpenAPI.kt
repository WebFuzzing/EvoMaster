package org.evomaster.core.problem.rest.schema

import io.swagger.v3.oas.models.OpenAPI

class SchemaOpenAPI(
    /**
     * The actual raw value of the schema file, as a string
     */
    val schemaRaw : String,
    /**
     * A parsed schema
     */
    val schemaParsed: OpenAPI,
    /**
     * the location the schema was retrieved from.
     * It can be null, eg, if provided as string in our own EM tests
     */
    val sourceLocation: SchemaLocation
)