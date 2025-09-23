package org.evomaster.client.java.controller.opensearch.selectors;

import org.evomaster.client.java.controller.opensearch.operations.RegexpOperation;
import org.evomaster.client.java.controller.opensearch.operations.QueryOperation;
import org.evomaster.client.java.controller.opensearch.utils.ParameterExtractor;

/**
 * Selector for Regexp queries.
 * Structure: { regexp: { field: { value: "[a-zA-Z]amlet", flags: "ALL", ... } } }
 */
public class RegexpSelector extends SingleConditionQuerySelector {

    private static final String OPERATOR = "Regexp";
    private static final String STRUCTURE = "regexp";

    @Override
    protected QueryOperation parse(Object query) {
        ParameterExtractor.RegexpParams params = ParameterExtractor.extractRegexpParams(query, structure());
        return new RegexpOperation(params.baseParams.fieldName, params.baseParams.value, 
                                  params.baseParams.commonParams, params.flags, params.maxDeterminizedStates);
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
