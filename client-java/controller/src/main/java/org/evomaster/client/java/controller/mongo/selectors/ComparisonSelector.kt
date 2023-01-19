package org.evomaster.client.java.controller.mongo.selectors

import org.bson.Document
import org.evomaster.client.java.controller.mongo.operations.ComparisonOperation
import org.evomaster.client.java.controller.mongo.operations.QueryOperation

abstract class ComparisonSelector: QuerySelector() {
    abstract fun operator(): String

    abstract fun <V> operation(fieldName: String, value: V): ComparisonOperation<V>

    override fun getOperation(query: Document): QueryOperation? {
        val fieldName = query.keys.first()
        val value = query[fieldName]

        if (value is Document) {
            if (isUniqueEntry(value)) {
                val actualOperator = value.keys.first()
                val comparisonValue = value[actualOperator]
                if(actualOperator == operator()) return operation(fieldName, comparisonValue)
            }
        }else{
            if(value !is ArrayList<*>)
            // Implicit $eq
            return operation(fieldName, value)
        }

        return null
    }
}