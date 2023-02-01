package org.evomaster.client.java.controller.mongo.selectors

import org.bson.Document
import org.evomaster.client.java.controller.mongo.operations.*
import org.evomaster.core.mongo.QueryParser

class ElemMatchSelector : SingleConditionQuerySelector() {
    override fun parseValue(fieldName: String, value: Any?): QueryOperation? {
        return when (value) {
            is Document -> {
                val filter = QueryParser().parse(value)
                ElemMatchOperation(fieldName, filter)
            }
            else -> null
        }
    }
    override fun operator(): String = "\$elemMatch"
}