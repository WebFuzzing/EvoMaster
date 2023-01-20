package org.evomaster.client.java.controller.mongo.selectors

import org.evomaster.client.java.controller.mongo.operations.*

class ExistsSelector : SingleConditionQuerySelector() {
    override fun operator(): String = "\$exists"

    override fun parseValue(fieldName: String, value: Any?): QueryOperation? {
        return when (value) {
            is Boolean -> ExistsOperation(fieldName, value)
            else -> null
        }
    }
}