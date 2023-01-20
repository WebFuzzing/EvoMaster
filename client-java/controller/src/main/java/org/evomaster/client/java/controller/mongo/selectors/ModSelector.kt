package org.evomaster.client.java.controller.mongo.selectors

import org.evomaster.client.java.controller.mongo.operations.*

class ModSelector : SingleConditionQuerySelector() {
    override fun operator(): String = "\$mod"

    override fun parseValue(fieldName: String, value: Any?): QueryOperation? {
        return when (value) {
            // Validate remainder and divisor
            is List<*> -> ModOperation(fieldName, value[0] as Long, value[1] as Long)
            else -> null
        }
    }
}