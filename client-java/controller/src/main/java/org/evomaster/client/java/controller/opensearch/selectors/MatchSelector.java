package org.evomaster.client.java.controller.opensearch.selectors;

import org.evomaster.client.java.controller.opensearch.operations.MatchOperation;
import org.evomaster.client.java.controller.opensearch.operations.QueryOperation;
import org.evomaster.client.java.controller.opensearch.utils.ParameterExtractor;

/**
 * Selector for Match queries.
 * Structure: { match: { field: { query: "text", operator: "and", ... } } }
 */
public class MatchSelector extends SingleConditionQuerySelector {

    private static final String OPERATOR = "Match";
    private static final String STRUCTURE = "match";

    @Override
    protected QueryOperation parse(Object query) {
        ParameterExtractor.MatchParams params = ParameterExtractor.extractMatchParams(query, structure());
        
        return new MatchOperation(params.baseParams.fieldName, params.baseParams.value, 
                                 params.baseParams.commonParams, params.operator, params.minimumShouldMatch,
                                 params.fuzziness, params.prefixLength, params.maxExpansions, params.analyzer,
                                 params.fuzzyTranspositions, params.lenient, params.zeroTermsQuery, null);
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
