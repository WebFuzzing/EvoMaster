package org.evomaster.client.java.controller.mongo.selectors

import org.evomaster.client.java.controller.mongo.operations.ComparisonOperation
import org.evomaster.client.java.controller.mongo.operations.GreaterThanOperation

class GreaterThanSelector : ComparisonSelector() {
    override fun operator(): String {
        return "\$gt"
    }

    override fun <V> operation(fieldName: String, value: V): ComparisonOperation<V> {
        return GreaterThanOperation(fieldName,value)
    }

}