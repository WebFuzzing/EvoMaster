package org.evomaster.client.java.controller.mongo.operations

/**
 * Represent $gte operation.
 * Selects the documents where the value of the field is greater than or equal to a specified value
 */
class GreaterThanEqualsOperation<V>(
    override val fieldName: String,
    override val value: V
) : ComparisonOperation<V>(fieldName, value)