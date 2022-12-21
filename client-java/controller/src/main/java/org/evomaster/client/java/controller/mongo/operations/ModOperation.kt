package org.evomaster.client.java.controller.mongo.operations

open class ModOperation(
    open val fieldName: String,
    open val divisor: Long,
    open val remainder: Long
) : QueryOperation()