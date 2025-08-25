package org.evomaster.client.java.controller.opensearch.selectors;

import org.evomaster.client.java.controller.opensearch.operations.TermOperation;
import org.evomaster.client.java.controller.opensearch.operations.QueryOperation;

/**
 * { term: { field: value } }
 */
public class TermSelector extends SingleConditionQuerySelector {

    @Override
    protected QueryOperation parseValue(String fieldName, Object value) {
        return new TermOperation<>(fieldName, value);
    }

    @Override
    protected String operator() {
        return "Term";
    }

    @Override
    protected String structure() {
        return "term";
    }
}