package org.evomaster.client.java.controller.mongo.selectors

import org.bson.Document
import org.evomaster.client.java.controller.mongo.operations.*

class NotInSelector : QuerySelector() {
    override fun getOperation(query: Document): QueryOperation? {
        if (!isUniqueEntry(query)) return null

        val fieldName = query.keys.first()
        val notInOperator = (query[fieldName] as Document).keys.first()

        if (notInOperator != "\$nin") return null

        val values = (query[fieldName] as Document)[notInOperator] as List<*>

        return NotInOperation(fieldName, values)
    }
}