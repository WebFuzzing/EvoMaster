package org.evomaster.client.java.controller.mongo.selectors

import org.bson.Document
import org.evomaster.client.java.controller.mongo.operations.*
import org.evomaster.core.mongo.QueryParser

class NotSelector : SingleConditionQuerySelector() {
    override fun operator(): String = "\$not"

    override fun parseValue(fieldName: String, value: Any?): QueryOperation? {
        return when (value) {
            is Document -> {
                // This is necessary for query parser to work correctly as the syntax for not is different
                // The field is at the beginning instead
                val docWithRemovedNot = Document().append(fieldName, value)
                val filter = QueryParser().parse(docWithRemovedNot)
                return NotOperation(fieldName, filter)
            }
            else -> null
        }
    }
}