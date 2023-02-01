package org.evomaster.client.java.controller.mongo.operations.synthetic

import org.evomaster.client.java.controller.mongo.operations.QueryOperation

open class InvertedModOperation(
    open val fieldName: String,
    open val divisor: Long,
    open val remainder: Long
) : QueryOperation()