package org.evomaster.client.java.controller.opensearch.selectors;

import org.evomaster.client.java.controller.opensearch.operations.BoolOperation;
import org.evomaster.client.java.controller.opensearch.operations.QueryOperation;
import org.evomaster.client.java.controller.opensearch.OpenSearchQueryParser;

import java.util.ArrayList;
import java.util.List;

import static org.evomaster.client.java.controller.opensearch.utils.OpenSearchQueryHelper.extractBoolClause;
import static org.evomaster.client.java.controller.opensearch.utils.OpenSearchQueryHelper.extractBoost;
import static org.evomaster.client.java.controller.opensearch.utils.OpenSearchQueryHelper.extractIntegerParameter;

/**
 * Selector for Bool queries.
 * Structure: { bool: { must: [...], must_not: [...], should: [...], filter: [...] } }
 */
public class BoolSelector extends SingleConditionQuerySelector {

    private static final String OPERATOR = "Bool";
    private static final String STRUCTURE = "bool";

    @Override
    protected QueryOperation parse(Object query) {
        List<QueryOperation> must = parseNestedQueries(extractBoolClause(query, structure(), "must"));
        List<QueryOperation> mustNot = parseNestedQueries(extractBoolClause(query, structure(), "mustNot"));
        List<QueryOperation> should = parseNestedQueries(extractBoolClause(query, structure(), "should"));
        List<QueryOperation> filter = parseNestedQueries(extractBoolClause(query, structure(), "filter"));
        
        Integer minimumShouldMatch = extractIntegerParameter(query, structure(), "minimumShouldMatch");
        Float boost = extractBoost(query, structure());
        
        return new BoolOperation(must, mustNot, should, filter, minimumShouldMatch, boost);
    }

    private List<QueryOperation> parseNestedQueries(List<Object> queryObjects) {
        List<QueryOperation> operations = new ArrayList<>();
        OpenSearchQueryParser parser = new OpenSearchQueryParser();
        
        for (Object queryObj : queryObjects) {
            QueryOperation operation = parser.parse(queryObj);
            if (operation != null) {
                operations.add(operation);
            }
        }
        
        return operations;
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
