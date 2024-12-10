package org.evomaster.client.java.controller.mongo.selectors;

import org.evomaster.client.java.controller.mongo.operations.QueryOperation;

import java.util.Map;

/**
 * A selector is used to determine if a query correspond to a certain operation.
 * To do so it checks that the syntax is correct (operator, value type, etc.).
 * Each selector maps to a unique operation.
 */
abstract public class QuerySelector {
    /**
     * Returns the operation mapped to the selector if the query satisfy all syntax checks or null if not.
     */
    public abstract QueryOperation getOperation(Object query);

    protected Boolean isUniqueEntry(Map<?, ?> map) {
        return map.size() == 1;
    }

    protected Boolean hasTheExpectedOperator(Object query) {
        String actualOperator = extractOperator(query);
        return actualOperator.equals(operator());
    }

    /**
     * Extracts the operator (for example $eq) from the query.
     */
    protected abstract String extractOperator(Object query);

    /**
     * The operator a query must have to be considered as an instance of the corresponding operation.
     */
    protected abstract String operator();
}