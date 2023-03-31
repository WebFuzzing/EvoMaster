package org.evomaster.client.java.controller.mongo.operations

/**
 * Represent $lt operation.
 * Selects the documents where the value of the field is less than to a specified value
 */
class LessThanOperation<V>(
    override val fieldName: String,
    override val value: V
): ComparisonOperation<V>(fieldName, value)