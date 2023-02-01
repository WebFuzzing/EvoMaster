package org.evomaster.client.java.controller.mongo.selectors

import org.evomaster.client.java.controller.mongo.operations.*

class NotInSelector : SingleConditionQuerySelector() {
    override fun operator(): String = "\$nin"

    override fun parseValue(fieldName: String, value: Any?): QueryOperation? {
        return when (value) {
            is ArrayList<*> -> NotInOperation(fieldName, value)
            else -> null
        }
    }
}