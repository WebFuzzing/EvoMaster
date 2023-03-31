package org.evomaster.client.java.controller.mongo.operations

import org.bson.conversions.Bson

/**
 * Represent $jsonSchema operation.
 * Matches documents that satisfy the specified JSON Schema.
 */
open class JsonSchemaOperation (open val schema: Bson) : QueryOperation()