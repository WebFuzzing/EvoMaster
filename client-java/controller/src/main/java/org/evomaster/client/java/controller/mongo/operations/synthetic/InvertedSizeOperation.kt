package org.evomaster.client.java.controller.mongo.operations.synthetic

import org.evomaster.client.java.controller.mongo.operations.QueryOperation

/**
 * Represent the operation that results from applying a $not to a $size operation.
 * It's synthetic as there is no operator defined in the spec with this behaviour.
 * Selects the documents that do not match the $size operation.
 */
open class InvertedSizeOperation(
    open val fieldName: String,
    open val value: Int
) : QueryOperation()