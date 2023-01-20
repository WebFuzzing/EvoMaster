package org.evomaster.client.java.controller.mongo.selectors

import org.bson.Document
import org.evomaster.client.java.controller.mongo.operations.QueryOperation

abstract class QuerySelector {
    abstract fun getOperation(query: Document): QueryOperation?

    protected fun isUniqueEntry(map: Map<*, *>) = map.size == 1

    protected fun hasTheExpectedOperator(query: Document): Boolean {
        val actualOperator = extractOperator(query)
        return actualOperator == operator()
    }

    protected abstract fun extractOperator(query: Document): String

    protected abstract fun operator(): String
}