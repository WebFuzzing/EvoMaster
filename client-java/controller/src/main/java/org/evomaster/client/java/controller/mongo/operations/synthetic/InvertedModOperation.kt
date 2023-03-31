package org.evomaster.client.java.controller.mongo.operations.synthetic

import org.evomaster.client.java.controller.mongo.operations.QueryOperation

/**
 * Represent the operation that results from applying a $not to a $mod operation.
 * It's synthetic as there is no operator defined in the spec with this behaviour.
 * Selects the documents that do not match the $mod operation.
 */
open class InvertedModOperation(
    open val fieldName: String,
    open val divisor: Long,
    open val remainder: Long
) : QueryOperation()