package org.evomaster.client.java.controller.mongo.operations

class NotEqualsOperation<V>(
    override val fieldName: String,
    override val value: V
): ComparisonOperation<V>(fieldName, value)