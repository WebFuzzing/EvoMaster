package org.evomaster.client.java.controller.mongo.selectors

import org.evomaster.client.java.controller.mongo.operations.ComparisonOperation
import org.evomaster.client.java.controller.mongo.operations.GreaterThanEqualsOperation

class GreaterThanEqualsSelector : ComparisonSelector() {
    override fun operator(): String {
        return "\$gte"
    }

    override fun <V> operation(fieldName: String, value: V): ComparisonOperation<V> {
        return GreaterThanEqualsOperation(fieldName,value)
    }

}