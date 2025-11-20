package org.evomaster.client.java.controller.opensearch.selectors;

import org.evomaster.client.java.controller.opensearch.operations.RangeOperation;
import org.evomaster.client.java.controller.opensearch.operations.QueryOperation;

import static org.evomaster.client.java.controller.opensearch.utils.OpenSearchQueryHelper.extractFieldName;
import static org.evomaster.client.java.controller.opensearch.utils.OpenSearchQueryHelper.extractRangeParameter;
import static org.evomaster.client.java.controller.opensearch.utils.OpenSearchQueryHelper.extractRangeStringParameter;
import static org.evomaster.client.java.controller.opensearch.utils.OpenSearchQueryHelper.extractBoost;

/**
 * Selector for Range queries.
 * Structure: { range: { field: { gte: value, lte: value, ... } } }
 */
public class RangeSelector extends SingleConditionQuerySelector {

    private static final String OPERATOR = "Range";
    private static final String STRUCTURE = "range";

    @Override
    protected QueryOperation parse(Object query) {
        String fieldName = extractFieldName(query, structure());
        Object gte = extractRangeParameter(query, structure(), "gte");
        Object gt = extractRangeParameter(query, structure(), "gt");
        Object lte = extractRangeParameter(query, structure(), "lte");
        Object lt = extractRangeParameter(query, structure(), "lt");
        String format = extractRangeStringParameter(query, structure(), "format");
        String relation = extractRangeStringParameter(query, structure(), "relation");
        Float boost = extractBoost(query, structure());
        String timeZone = extractRangeStringParameter(query, structure(), "timeZone");
        
        return new RangeOperation(fieldName, gte, gt, lte, lt, format, relation, boost, timeZone);
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
