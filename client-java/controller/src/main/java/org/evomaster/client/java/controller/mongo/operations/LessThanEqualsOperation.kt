package org.evomaster.client.java.controller.mongo.operations

/**
 * Represent $lte operation.
 * Selects the documents where the value of the field is less than or equal to a specified value
 */
class LessThanEqualsOperation<V>(
    override val fieldName: String,
    override val value: V
): ComparisonOperation<V>(fieldName, value)