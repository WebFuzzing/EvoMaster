package org.evomaster.client.java.controller.mongo.selectors

import org.evomaster.client.java.controller.mongo.operations.GreaterThanOperation
import org.evomaster.client.java.controller.mongo.operations.QueryOperation

/**
 * { field: { $gt: value } }
 */
class GreaterThanSelector : SingleConditionQuerySelector() {
    override fun operator(): String {
        return "\$gt"
    }

    override fun parseValue(fieldName: String, value: Any?): QueryOperation = GreaterThanOperation(fieldName,value)
}