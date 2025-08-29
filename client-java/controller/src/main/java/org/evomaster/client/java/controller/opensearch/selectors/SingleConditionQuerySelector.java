package org.evomaster.client.java.controller.opensearch.selectors;

import org.evomaster.client.java.controller.opensearch.operations.QueryOperation;

import static org.evomaster.client.java.controller.opensearch.utils.OpenSearchQueryHelper.extractQueryKind;

/**
 * Selectors for operations whose value consist of a single condition
 */
abstract class SingleConditionQuerySelector extends QuerySelector {
    @Override
    public QueryOperation getOperation(Object query) {
        if (!hasTheExpectedOperator(query)) return null;
        return parse(query);
    }

    protected String extractOperator(Object query) {
        return extractQueryKind(query);
    }

    protected abstract QueryOperation parse(Object query);
}