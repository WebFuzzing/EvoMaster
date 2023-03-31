package org.evomaster.client.java.controller.mongo.operations.synthetic

import org.bson.BsonType
import org.evomaster.client.java.controller.mongo.operations.QueryOperation

/**
 * Represent the operation that results from applying a $not to a type operation.
 * It's synthetic as there is no operator defined in the spec with this behaviour.
 * Selects the documents that do not match the $type operation.
 */
open class InvertedTypeOperation (open val fieldName: String, open val type: BsonType) : QueryOperation()