package org.evomaster.client.java.controller.mongo.selectors

import org.evomaster.client.java.controller.mongo.operations.GreaterThanEqualsOperation
import org.evomaster.client.java.controller.mongo.operations.QueryOperation

class GreaterThanEqualsSelector : SingleConditionQuerySelector() {
    override fun operator(): String {
        return "\$gte"
    }

    override fun parseValue(fieldName: String, value: Any?): QueryOperation = GreaterThanEqualsOperation(fieldName,value)
}