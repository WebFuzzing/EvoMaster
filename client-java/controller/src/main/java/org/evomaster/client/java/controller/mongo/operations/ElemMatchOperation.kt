package org.evomaster.client.java.controller.mongo.operations

open class ElemMatchOperation(open val fieldName: String, open val filter: QueryOperation) :
    QueryOperation() {
}