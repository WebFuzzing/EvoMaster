package org.evomaster.client.java.controller.mongo.selectors

import org.bson.Document
import org.evomaster.client.java.controller.mongo.operations.*
import org.evomaster.core.mongo.QueryParser

class OrSelector : MultiConditionQuerySelector() {
    override fun operator(): String = "\$or"

    override fun parseConditions(value: Any?): QueryOperation? {
        return when (value) {
            is List<*> -> {
                val filters = value.map { filter -> QueryParser().parse(filter as Document)}
                OrOperation(filters)
            }
            else -> null
        }
    }
}