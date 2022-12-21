package org.evomaster.client.java.controller.mongo.selectors

import org.bson.Document
import org.evomaster.client.java.controller.mongo.operations.*
import org.evomaster.core.mongo.QueryParser

class ElemMatchSelector : QuerySelector() {
    override fun getOperation(query: Document): QueryOperation? {
        if (!isUniqueEntry(query)) return null

        val elemMatchOperator = query.keys.first()

        if (elemMatchOperator != "\$elemMatch") return null

        val filters = (query[elemMatchOperator] as List<*>).map { filter -> QueryParser().parse(filter as Document)}

        return ElemMatchOperation(filters)
    }
}