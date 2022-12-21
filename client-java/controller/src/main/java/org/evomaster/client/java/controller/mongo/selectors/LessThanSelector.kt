package org.evomaster.client.java.controller.mongo.selectors

import org.evomaster.client.java.controller.mongo.operations.ComparisonOperation
import org.evomaster.client.java.controller.mongo.operations.LessThanOperation

class LessThanSelector : ComparisonSelector() {
    override fun operator(): String {
        return "\$lt"
    }

    override fun <V> operation(fieldName: String, value: V): ComparisonOperation<V> {
        return LessThanOperation(fieldName,value)
    }

}