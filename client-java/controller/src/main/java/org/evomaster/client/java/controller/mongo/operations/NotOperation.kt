package org.evomaster.client.java.controller.mongo.operations

/**
 * Represent $not operation.
 * Selects the documents that do not match the condition.
 */
 open class NotOperation(open val fieldName: String, open val condition: QueryOperation) : QueryOperation() {
}