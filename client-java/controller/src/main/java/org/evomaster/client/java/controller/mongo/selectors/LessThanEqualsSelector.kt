package org.evomaster.client.java.controller.mongo.selectors

import org.evomaster.client.java.controller.mongo.operations.ComparisonOperation
import org.evomaster.client.java.controller.mongo.operations.LessThanEqualsOperation

class LessThanEqualsSelector : ComparisonSelector() {
    override fun operator(): String {
        return "\$lte"
    }

    override fun <V> operation(fieldName: String, value: V): ComparisonOperation<V> {
        return LessThanEqualsOperation(fieldName,value)
    }

}