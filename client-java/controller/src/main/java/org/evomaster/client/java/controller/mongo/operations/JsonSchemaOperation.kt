package org.evomaster.client.java.controller.mongo.operations

import org.bson.conversions.Bson

open class JsonSchemaOperation (open val schema: Bson) : QueryOperation()