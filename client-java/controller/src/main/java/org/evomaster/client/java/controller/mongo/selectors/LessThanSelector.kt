package org.evomaster.client.java.controller.mongo.selectors

import org.evomaster.client.java.controller.mongo.operations.LessThanOperation
import org.evomaster.client.java.controller.mongo.operations.QueryOperation

class LessThanSelector : SingleConditionQuerySelector() {
    override fun operator(): String {
        return "\$lt"
    }

    override fun parseValue(fieldName: String, value: Any?): QueryOperation = LessThanOperation(fieldName,value)
}