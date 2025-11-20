package org.evomaster.client.java.controller.opensearch.selectors;

import org.evomaster.client.java.controller.opensearch.operations.WildcardOperation;
import org.evomaster.client.java.controller.opensearch.operations.QueryOperation;
import org.evomaster.client.java.controller.opensearch.utils.ParameterExtractor;

/**
 * Selector for Wildcard queries.
 * Structure: { wildcard: { field: { value: "pattern*", case_insensitive: false, ... } } }
 */
public class WildcardSelector extends SingleConditionQuerySelector {

    private static final String OPERATOR = "Wildcard";
    private static final String STRUCTURE = "wildcard";

    @Override
    protected QueryOperation parse(Object query) {
        ParameterExtractor.FieldValueParams params = ParameterExtractor.extractFieldValueParams(query, structure());
        return new WildcardOperation(params.fieldName, params.value, params.commonParams);
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
