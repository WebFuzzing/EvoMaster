package org.evomaster.client.java.controller.opensearch.selectors;

import org.evomaster.client.java.controller.opensearch.operations.TermOperation;
import org.evomaster.client.java.controller.opensearch.operations.QueryOperation;

import static org.evomaster.client.java.controller.opensearch.utils.OpenSearchQueryHelper.extractFieldName;
import static org.evomaster.client.java.controller.opensearch.utils.OpenSearchQueryHelper.extractFieldValue;

/**
 * { term: { field: value } }
 */
public class TermSelector extends SingleConditionQuerySelector {

    private static final String OPERATOR = "Term";
    private static final String STRUCTURE = "term";

    @Override
    protected QueryOperation parse(Object query) {
        String fieldName = extractFieldName(query, structure());
        Object value = extractFieldValue(query, structure());
        return new TermOperation<>(fieldName, value);
    }

    @Override
    protected String operator() {
        return OPERATOR;
    }

    @Override
    protected String structure() {
        return STRUCTURE;
    }
}