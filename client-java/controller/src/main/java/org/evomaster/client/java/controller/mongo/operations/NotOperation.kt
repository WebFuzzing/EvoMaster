package org.evomaster.client.java.controller.mongo.operations

 open class NotOperation(open val fieldName: String, open val filter: QueryOperation) : QueryOperation() {
}