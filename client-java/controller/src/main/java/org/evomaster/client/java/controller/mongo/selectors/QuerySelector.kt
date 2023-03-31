package org.evomaster.client.java.controller.mongo.selectors

import org.bson.Document
import org.evomaster.client.java.controller.mongo.operations.QueryOperation

/**
 * A selector is used to determine if a query correspond to a certain operation.
 * To do so it checks that the syntax is correct (operator, value type, etc.).
 * Each selector maps to a unique operation.
 */
abstract class QuerySelector {
    /**
     * Returns the operation mapped to the selector if the query satisfy all syntax checks or null if not.
     */
    abstract fun getOperation(query: Document): QueryOperation?

    protected fun isUniqueEntry(map: Map<*, *>) = map.size == 1
    protected fun hasTheExpectedOperator(query: Document): Boolean {
        val actualOperator = extractOperator(query)
        return actualOperator == operator()
    }

    /**
     * Extracts the operator (for example $eq) from the query.
     */
    protected abstract fun extractOperator(query: Document): String

    /**
     * The operator a query must have to be considered as an instance of the corresponding operation.
     */
    protected abstract fun operator(): String
}