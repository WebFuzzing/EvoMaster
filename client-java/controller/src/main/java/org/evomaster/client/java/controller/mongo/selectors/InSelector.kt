package org.evomaster.client.java.controller.mongo.selectors

import org.evomaster.client.java.controller.mongo.operations.*

/**
 * { field: { $in: [<value1>, <value2>, ... <valueN> ] } }
 */
class InSelector : SingleConditionQuerySelector() {
    override fun operator(): String = "\$in"

    override fun parseValue(fieldName: String, value: Any?): QueryOperation? {
        return when (value) {
            is ArrayList<*> -> InOperation(fieldName, value)
            else -> null
        }
    }
}