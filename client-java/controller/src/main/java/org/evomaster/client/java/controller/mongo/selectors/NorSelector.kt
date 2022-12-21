package org.evomaster.client.java.controller.mongo.selectors

import org.bson.Document
import org.evomaster.client.java.controller.mongo.operations.*
import org.evomaster.core.mongo.QueryParser

class NorSelector : QuerySelector() {
    override fun getOperation(query: Document): QueryOperation? {
        if (!isUniqueEntry(query)) return null

        val norOperator = query.keys.first()

        if (norOperator != "\$nor") return null

        val filters = (query[norOperator] as List<*>).map { filter -> QueryParser().parse(filter as Document)}

        return NorOperation(filters)
    }
}