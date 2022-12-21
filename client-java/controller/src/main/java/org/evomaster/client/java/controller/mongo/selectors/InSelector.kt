package org.evomaster.client.java.controller.mongo.selectors

import org.bson.Document
import org.evomaster.client.java.controller.mongo.operations.*

class InSelector : QuerySelector() {
    override fun getOperation(query: Document): QueryOperation? {
        if (!isUniqueEntry(query)) return null

        val fieldName = query.keys.first()
        val inOperator = (query[fieldName] as Document).keys.first()

        if (inOperator != "\$in") return null

        val values = (query[fieldName] as Document)[inOperator] as List<*>

        return InOperation(fieldName, values)
    }
}