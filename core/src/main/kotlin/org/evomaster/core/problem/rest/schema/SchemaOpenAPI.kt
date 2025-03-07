package org.evomaster.core.problem.rest.schema

import io.swagger.v3.oas.models.OpenAPI

class SchemaOpenAPI(
    val schemaRaw : String,
    val schemaParsed: OpenAPI
)