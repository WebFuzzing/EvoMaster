package org.evomaster.client.java.controller.mongo.selectors

import org.bson.Document
import org.evomaster.client.java.controller.mongo.operations.*
import org.evomaster.core.mongo.QueryParser

class OrSelector : QuerySelector() {
    override fun getOperation(query: Document): QueryOperation? {
        if (!isUniqueEntry(query)) return null

        val orOperator = query.keys.first()

        if (orOperator != "\$or") return null

        val filters = (query[orOperator] as List<*>).map { filter -> QueryParser().parse(filter as Document)}

        return OrOperation(filters)
    }
}