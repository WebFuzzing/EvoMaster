package org.evomaster.client.java.controller.mongo.operations.synthetic

import org.evomaster.client.java.controller.mongo.operations.QueryOperation

/**
 * Represent the operation that results from applying a $not to an $all operation.
 * It's synthetic as there is no operator defined in the spec with this behaviour.
 * Selects the documents that do not match the $all operation.
 */
open class InvertedAllOperation<V>(
    open val fieldName: String,
    open val values: ArrayList<V>
) : QueryOperation()