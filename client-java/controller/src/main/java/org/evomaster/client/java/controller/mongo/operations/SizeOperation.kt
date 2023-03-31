package org.evomaster.client.java.controller.mongo.operations

/**
 * Represent $size operation.
 * Matches any array with the number of elements specified by the argument.
 */
open class SizeOperation(
    open val fieldName: String,
    open val value: Int
) : QueryOperation()