package org.evomaster.client.java.controller.opensearch.selectors;

import org.evomaster.client.java.controller.opensearch.operations.TermsOperation;
import org.evomaster.client.java.controller.opensearch.operations.QueryOperation;

import java.util.List;

import static org.evomaster.client.java.controller.opensearch.utils.OpenSearchQueryHelper.extractFieldName;
import static org.evomaster.client.java.controller.opensearch.utils.OpenSearchQueryHelper.extractTermsArray;
import static org.evomaster.client.java.controller.opensearch.utils.OpenSearchQueryHelper.extractBoost;
import static org.evomaster.client.java.controller.opensearch.utils.OpenSearchQueryHelper.extractQueryName;
import static org.evomaster.client.java.controller.opensearch.utils.OpenSearchQueryHelper.extractValueType;

/**
 * Selector for Terms queries.
 * Structure: { terms: { field: [value1, value2, ...] } }
 */
public class TermsSelector extends SingleConditionQuerySelector {

    private static final String OPERATOR = "Terms";
    private static final String STRUCTURE = "terms";

    @Override
    protected QueryOperation parse(Object query) {
        String fieldName = extractFieldName(query, structure());
        List<Object> values = extractTermsArray(query, structure());
        Float boost = extractBoost(query, structure());
        String name = extractQueryName(query, structure());
        String valueType = extractValueType(query, structure());
        
        return new TermsOperation<>(fieldName, values, boost, name, valueType);
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
