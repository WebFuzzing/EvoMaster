package org.evomaster.client.java.controller.mongo.selectors

import org.evomaster.client.java.controller.mongo.operations.EqualsOperation
import org.evomaster.client.java.controller.mongo.operations.QueryOperation

class EqualsSelector : SingleConditionQuerySelector() {
    override fun operator(): String {
        return "\$eq"
    }

    override fun parseValue(fieldName: String, value: Any?): QueryOperation = EqualsOperation(fieldName,value)
}