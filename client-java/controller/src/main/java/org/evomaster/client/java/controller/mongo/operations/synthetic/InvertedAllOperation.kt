package org.evomaster.client.java.controller.mongo.operations.synthetic

import org.evomaster.client.java.controller.mongo.operations.QueryOperation

open class InvertedAllOperation<V>(
    open val fieldName: String,
    open val values: ArrayList<V>
) : QueryOperation()