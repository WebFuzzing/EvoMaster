package org.evomaster.client.java.controller.mongo.selectors

import org.bson.Document
import org.evomaster.client.java.controller.mongo.operations.*

/**
 * Selectors for operations whose value consist of a single condition
 */
abstract class SingleConditionQuerySelector : QuerySelector() {
    override fun getOperation(query: Document): QueryOperation? {
        val fieldName = extractFieldName(query)
        if (!isUniqueEntry(query) || query[fieldName] !is Document || !hasTheExpectedOperator(query)) return null
        val value = (query[fieldName] as Document)[operator()]
        return parseValue(fieldName, value)
    }

    override fun extractOperator(query: Document): String {
        val fieldName = extractFieldName(query)
        return  (query[fieldName] as Document).keys.first()
    }

    protected abstract fun parseValue(fieldName: String, value: Any?): QueryOperation?

    private fun extractFieldName(query: Document): String = query.keys.first()
}