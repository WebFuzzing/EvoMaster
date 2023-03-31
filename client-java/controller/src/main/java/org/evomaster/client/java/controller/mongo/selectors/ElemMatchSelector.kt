package org.evomaster.client.java.controller.mongo.selectors

import org.bson.Document
import org.evomaster.client.java.controller.mongo.operations.*
import org.evomaster.client.java.controller.mongo.QueryParser

/**
 * { field: { $elemMatch: { <query1>, <query2>, ... } } }
 */
class ElemMatchSelector : SingleConditionQuerySelector() {
    override fun parseValue(fieldName: String, value: Any?): QueryOperation? {
        return when (value) {
            is Document -> {
                val condition = QueryParser().parse(value)
                ElemMatchOperation(fieldName, condition)
            }
            else -> null
        }
    }
    override fun operator(): String = "\$elemMatch"
}