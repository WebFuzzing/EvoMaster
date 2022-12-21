package org.evomaster.client.java.controller.mongo.operations

abstract class ComparisonOperation<V>(
    open val fieldName: String,
    open val value: V
): QueryOperation() {
}