package org.evomaster.client.java.controller.mongo.selectors

import org.bson.Document
import org.evomaster.client.java.controller.mongo.operations.*

class SizeSelector : QuerySelector() {
    override fun getOperation(query: Document): QueryOperation? {
        if (!isUniqueEntry(query)) return null

        val fieldName = query.keys.first()
        val sizeOperator = (query[fieldName] as Document).keys.first()

        if (sizeOperator != "\$size") return null

        val value = (query[fieldName] as Document)[sizeOperator]

        if(value !is Int) return null

        return SizeOperation(fieldName, value)
    }
}