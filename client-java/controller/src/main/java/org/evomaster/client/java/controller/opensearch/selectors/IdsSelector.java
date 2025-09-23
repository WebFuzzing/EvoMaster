package org.evomaster.client.java.controller.opensearch.selectors;

import org.evomaster.client.java.controller.opensearch.operations.IdsOperation;
import org.evomaster.client.java.controller.opensearch.operations.QueryOperation;

import java.util.List;

import static org.evomaster.client.java.controller.opensearch.utils.OpenSearchQueryHelper.extractIdsValues;
import static org.evomaster.client.java.controller.opensearch.utils.OpenSearchQueryHelper.extractBoost;

/**
 * Selector for IDs queries.
 * Structure: { ids: { values: ["id1", "id2", ...] } }
 */
public class IdsSelector extends SingleConditionQuerySelector {

    private static final String OPERATOR = "Ids";
    private static final String STRUCTURE = "ids";

    @Override
    protected QueryOperation parse(Object query) {
        List<String> values = extractIdsValues(query, structure());
        Float boost = extractBoost(query, structure());
        
        return new IdsOperation(values, boost);
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
