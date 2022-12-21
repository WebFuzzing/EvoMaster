package org.evomaster.client.java.controller.mongo.selectors

import org.evomaster.client.java.controller.mongo.operations.ComparisonOperation
import org.evomaster.client.java.controller.mongo.operations.NotEqualsOperation

class NotEqualsSelector : ComparisonSelector() {
    override fun operator(): String {
        return "\$ne"
    }

    override fun <V> operation(fieldName: String, value: V): ComparisonOperation<V> {
        return NotEqualsOperation(fieldName,value)
    }
}