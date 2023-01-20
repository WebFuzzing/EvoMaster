package org.evomaster.client.java.controller.mongo.selectors

import org.evomaster.client.java.controller.mongo.operations.*

class SizeSelector : SingleConditionQuerySelector() {
    override fun operator(): String = "\$size"

    override fun parseValue(fieldName: String, value: Any?): QueryOperation? {
        return when (value) {
            is Int -> SizeOperation(fieldName, value)
            else -> null
        }
    }
}