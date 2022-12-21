package org.evomaster.client.java.controller.mongo.selectors

import org.bson.Document
import org.evomaster.client.java.controller.mongo.operations.*

class ModSelector : QuerySelector() {
    override fun getOperation(query: Document): QueryOperation? {
        if (!isUniqueEntry(query)) return null

        val fieldName = query.keys.first()
        val modOperator = (query[fieldName] as Document).keys.first()

        if (modOperator != "\$mod") return null

        val values = (query[fieldName] as Document)[modOperator] as List<*>

        return ModOperation(fieldName, values[0] as Long, values[1] as Long)
    }
}