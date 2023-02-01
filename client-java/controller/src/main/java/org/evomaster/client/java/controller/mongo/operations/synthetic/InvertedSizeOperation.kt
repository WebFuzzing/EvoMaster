package org.evomaster.client.java.controller.mongo.operations.synthetic

import org.evomaster.client.java.controller.mongo.operations.QueryOperation

open class InvertedSizeOperation(
    open val fieldName: String,
    open val value: Int
) : QueryOperation()