package org.evomaster.client.java.controller.mongo.selectors

import org.bson.Document
import org.evomaster.client.java.controller.mongo.operations.*

class ExistsSelector : QuerySelector() {
    override fun getOperation(query: Document): QueryOperation? {
        if (!isUniqueEntry(query)) return null

        val fieldName = query.keys.first()
        val existsOperator = (query[fieldName] as Document).keys.first()

        if (existsOperator != "\$exists") return null

        val boolean = (query[fieldName] as Document)[existsOperator]

        if(boolean !is Boolean) return null

        return ExistsOperation(fieldName, boolean)
    }
}