package org.evomaster.client.java.controller.mongo.operations

/**
 * Represent $ne operation.
 * Selects the documents where the value of the field is not equal to the specified value.
 * This includes documents that do not contain the field.
 */
class NotEqualsOperation<V>(
    override val fieldName: String,
    override val value: V
): ComparisonOperation<V>(fieldName, value)