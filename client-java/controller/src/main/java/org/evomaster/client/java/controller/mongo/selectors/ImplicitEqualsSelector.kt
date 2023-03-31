package org.evomaster.client.java.controller.mongo.selectors

import org.bson.Document
import org.evomaster.client.java.controller.mongo.operations.*

/**
 * { field: value }
 */
class ImplicitEqualsSelector : QuerySelector() {
    override fun getOperation(query: Document): QueryOperation? {
        val fieldName = extractFieldName(query)
        if (!isUniqueEntry(query)) return null
        val value = query[fieldName]
        return EqualsOperation(fieldName, value)
    }

    override fun extractOperator(query: Document): String {
        val fieldName = extractFieldName(query)
        return  (query[fieldName] as Document).keys.first()
    }

    override fun operator(): String {
        return ""
    }

    private fun extractFieldName(query: Document): String = query.keys.first()
}