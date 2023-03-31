package org.evomaster.client.java.controller.mongo.operations

import org.bson.BsonType

/**
 * Represent $type operation.
 * Selects documents where the value of the field is an instance of the specified BSON type(s).
 */
open class TypeOperation (open val fieldName: String, open val type: BsonType) : QueryOperation()