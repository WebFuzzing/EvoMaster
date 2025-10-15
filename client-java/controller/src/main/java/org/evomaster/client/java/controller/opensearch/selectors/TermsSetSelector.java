package org.evomaster.client.java.controller.opensearch.selectors;

import org.evomaster.client.java.controller.opensearch.operations.TermsSetOperation;
import org.evomaster.client.java.controller.opensearch.operations.QueryOperation;

import java.util.List;

import static org.evomaster.client.java.controller.opensearch.utils.OpenSearchQueryHelper.extractFieldName;
import static org.evomaster.client.java.controller.opensearch.utils.OpenSearchQueryHelper.extractTermsArray;
import static org.evomaster.client.java.controller.opensearch.utils.OpenSearchQueryHelper.extractBoost;
import static org.evomaster.client.java.controller.opensearch.utils.OpenSearchQueryHelper.extractMinimumShouldMatchField;
import static org.evomaster.client.java.controller.opensearch.utils.OpenSearchQueryHelper.extractMinimumShouldMatchScript;

/**
 * Selector for Terms Set queries.
 * Structure: { terms_set: { field: { terms: [...], minimum_should_match_field: "field" } } }
 */
public class TermsSetSelector extends SingleConditionQuerySelector {

    private static final String OPERATOR = "TermsSet";
    private static final String STRUCTURE = "terms_set";

    @Override
    protected QueryOperation parse(Object query) {
        String fieldName = extractFieldName(query, structure());
        List<Object> terms = extractTermsArray(query, structure());
        String minimumShouldMatchField = extractMinimumShouldMatchField(query, structure());
        String minimumShouldMatchScript = extractMinimumShouldMatchScript(query, structure());
        Float boost = extractBoost(query, structure());
        
        return new TermsSetOperation<>(fieldName, terms, minimumShouldMatchField, minimumShouldMatchScript, boost);
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
