package org.evomaster.client.java.controller.mongo.selectors

import org.bson.Document
import org.evomaster.client.java.controller.mongo.operations.*

abstract class MultiConditionQuerySelector : QuerySelector() {
    override fun getOperation(query: Document): QueryOperation? {
        if (!isUniqueEntry(query) || !hasTheExpectedOperator(query)) return null
        val value = query[operator()]
        return parseConditions(value)
    }

    override fun extractOperator(query: Document): String = query.keys.first()
    protected abstract fun parseConditions(value: Any?): QueryOperation?
}