package org.evomaster.client.java.controller.mongo.selectors

import org.bson.Document
import org.evomaster.client.java.controller.mongo.operations.*
import org.evomaster.core.mongo.QueryParser

class AndSelector : QuerySelector() {
    override fun getOperation(query: Document): QueryOperation? {
        if (!isUniqueEntry(query)) return null

        val andOperator = query.keys.first()

        if (andOperator != "\$and") return null

        val filters = (query[andOperator] as List<*>).map { filter -> QueryParser().parse(filter as Document)}

        return AndOperation(filters)
    }
}