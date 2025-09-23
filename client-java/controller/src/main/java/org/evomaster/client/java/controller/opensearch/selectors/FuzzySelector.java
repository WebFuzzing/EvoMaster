package org.evomaster.client.java.controller.opensearch.selectors;

import org.evomaster.client.java.controller.opensearch.operations.FuzzyOperation;
import org.evomaster.client.java.controller.opensearch.operations.QueryOperation;
import org.evomaster.client.java.controller.opensearch.utils.ParameterExtractor;

/**
 * Selector for Fuzzy queries.
 * Structure: { fuzzy: { field: { value: "term", fuzziness: 2, ... } } }
 */
public class FuzzySelector extends SingleConditionQuerySelector {

    private static final String OPERATOR = "Fuzzy";
    private static final String STRUCTURE = "fuzzy";

    @Override
    protected QueryOperation parse(Object query) {
        ParameterExtractor.FuzzyParams params = ParameterExtractor.extractFuzzyParams(query, structure());
        return new FuzzyOperation(params.baseParams.fieldName, params.baseParams.value, 
                                 params.baseParams.commonParams.getBoost(), params.fuzziness, 
                                 params.maxExpansions, params.prefixLength, params.transpositions, 
                                 params.baseParams.commonParams.getRewrite());
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
