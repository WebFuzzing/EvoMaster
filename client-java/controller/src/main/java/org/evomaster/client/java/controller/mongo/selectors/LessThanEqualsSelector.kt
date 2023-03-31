package org.evomaster.client.java.controller.mongo.selectors

import org.evomaster.client.java.controller.mongo.operations.LessThanEqualsOperation
import org.evomaster.client.java.controller.mongo.operations.QueryOperation

/**
 * { field: { $lte: value } }
 */
class LessThanEqualsSelector : SingleConditionQuerySelector() {
    override fun operator(): String {
        return "\$lte"
    }

    override fun parseValue(fieldName: String, value: Any?): QueryOperation = LessThanEqualsOperation(fieldName,value)
}