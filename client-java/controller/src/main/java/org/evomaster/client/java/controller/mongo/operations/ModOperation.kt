package org.evomaster.client.java.controller.mongo.operations

/**
 * Represent $mod operation.
 * Select documents where the value of a field divided by a divisor has the specified remainder.
 */
open class ModOperation(
    open val fieldName: String,
    open val divisor: Long,
    open val remainder: Long
) : QueryOperation()