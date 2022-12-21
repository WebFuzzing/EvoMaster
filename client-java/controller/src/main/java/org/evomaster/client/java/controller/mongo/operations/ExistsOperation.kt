package org.evomaster.client.java.controller.mongo.operations

open class ExistsOperation(
    open val fieldName: String,
    open val boolean: Boolean
) : QueryOperation()