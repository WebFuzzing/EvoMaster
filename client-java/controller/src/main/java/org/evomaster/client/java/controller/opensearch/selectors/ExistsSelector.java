package org.evomaster.client.java.controller.opensearch.selectors;

import org.evomaster.client.java.controller.opensearch.operations.ExistsOperation;
import org.evomaster.client.java.controller.opensearch.operations.QueryOperation;

import static org.evomaster.client.java.controller.opensearch.utils.OpenSearchQueryHelper.extractExistsField;
import static org.evomaster.client.java.controller.opensearch.utils.OpenSearchQueryHelper.extractBoost;

/**
 * Selector for Exists queries.
 * Structure: { exists: { field: "fieldname" } }
 */
public class ExistsSelector extends SingleConditionQuerySelector {

    private static final String OPERATOR = "Exists";
    private static final String STRUCTURE = "exists";

    @Override
    protected QueryOperation parse(Object query) {
        String field = extractExistsField(query, structure());
        Float boost = extractBoost(query, structure());
        
        return new ExistsOperation(field, boost);
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
