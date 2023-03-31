package org.evomaster.client.java.controller.mongo.operations

/**
 * Represent $gt operation.
 * Selects the documents where the value of the field is greater than or equal to a specified value
 */
class GreaterThanOperation<V>(
    override val fieldName: String,
    override val value: V
): ComparisonOperation<V>(fieldName, value)