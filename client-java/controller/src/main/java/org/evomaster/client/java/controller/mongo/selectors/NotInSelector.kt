package org.evomaster.client.java.controller.mongo.selectors

import org.evomaster.client.java.controller.mongo.operations.*

/**
 * { field: { $nin: [ <value1>, <value2> ... <valueN> ] } }
 */
class NotInSelector : SingleConditionQuerySelector() {
    override fun operator(): String = "\$nin"

    override fun parseValue(fieldName: String, value: Any?): QueryOperation? {
        return when (value) {
            is ArrayList<*> -> NotInOperation(fieldName, value)
            else -> null
        }
    }
}