package org.evomaster.client.java.controller.mongo.selectors

import org.evomaster.client.java.controller.mongo.operations.*

/**
 * { field: { $all: [ <value1> , <value2> ... ] } }
 */
class AllSelector : SingleConditionQuerySelector() {
    override fun operator(): String = "\$all"

    override fun parseValue(fieldName: String, value: Any?): QueryOperation? {
        return when (value) {
            is ArrayList<*> -> AllOperation(fieldName, value)
            else -> null
        }
    }
}