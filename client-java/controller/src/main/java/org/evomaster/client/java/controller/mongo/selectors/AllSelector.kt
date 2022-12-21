package org.evomaster.client.java.controller.mongo.selectors

import org.bson.Document
import org.evomaster.client.java.controller.mongo.operations.*
import org.evomaster.core.mongo.QueryParser

class AllSelector : QuerySelector() {
    override fun getOperation(query: Document): QueryOperation? {
        if (!isUniqueEntry(query)) return null

        val fieldName = query.keys.first()
        val allOperator = (query[fieldName] as Document).keys.first()

        if (allOperator != "\$all") return null

        val values = (query[fieldName] as Document)[allOperator] as List<*>

        return AllOperation(fieldName, values)
    }
}