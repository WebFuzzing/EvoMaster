package org.evomaster.client.java.controller.opensearch.selectors;

import org.evomaster.client.java.controller.opensearch.operations.PrefixOperation;
import org.evomaster.client.java.controller.opensearch.operations.QueryOperation;
import org.evomaster.client.java.controller.opensearch.utils.ParameterExtractor;

/**
 * Selector for Prefix queries.
 * Structure: { prefix: { field: { value: "prefix" } } }
 */
public class PrefixSelector extends SingleConditionQuerySelector {

    private static final String OPERATOR = "Prefix";
    private static final String STRUCTURE = "prefix";

    @Override
    protected QueryOperation parse(Object query) {
        ParameterExtractor.FieldValueParams params = ParameterExtractor.extractFieldValueParams(query, structure());
        return new PrefixOperation(params.fieldName, params.value, params.commonParams);
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
