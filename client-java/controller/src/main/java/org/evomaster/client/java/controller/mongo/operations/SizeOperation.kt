package org.evomaster.client.java.controller.mongo.operations

open class SizeOperation(
    open val fieldName: String,
    open val value: Int
) : QueryOperation()