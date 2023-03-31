package org.evomaster.client.java.controller.mongo.selectors

import org.evomaster.client.java.controller.mongo.operations.NotEqualsOperation
import org.evomaster.client.java.controller.mongo.operations.QueryOperation

/**
 * { field: { $ne: value } }
 */
class NotEqualsSelector : SingleConditionQuerySelector() {
    override fun operator(): String {
        return "\$ne"
    }

    override fun parseValue(fieldName: String, value: Any?): QueryOperation = NotEqualsOperation(fieldName,value)
}