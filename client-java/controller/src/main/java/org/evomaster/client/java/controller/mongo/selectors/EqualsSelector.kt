package org.evomaster.client.java.controller.mongo.selectors

import org.evomaster.client.java.controller.mongo.operations.ComparisonOperation
import org.evomaster.client.java.controller.mongo.operations.EqualsOperation

class EqualsSelector : ComparisonSelector() {
    override fun operator(): String {
        return "\$eq"
    }

    override fun <V> operation(fieldName: String, value: V): ComparisonOperation<V> {
        return EqualsOperation(fieldName,value)
    }

}