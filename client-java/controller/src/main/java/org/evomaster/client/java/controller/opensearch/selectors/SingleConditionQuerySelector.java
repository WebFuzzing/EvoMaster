package org.evomaster.client.java.controller.opensearch.selectors;

import static org.evomaster.client.java.controller.mongo.utils.BsonHelper.*;
import static org.evomaster.client.java.controller.opensearch.utils.OpenSearchQueryHelper.extractQueryKind;
import static org.evomaster.client.java.controller.opensearch.utils.OpenSearchQueryHelper.extractFieldName;
import static org.evomaster.client.java.controller.opensearch.utils.OpenSearchQueryHelper.extractFieldValue;

import java.util.Map;
import java.util.Set;
import org.evomaster.client.java.controller.opensearch.operations.*;

/**
 * Selectors for operations whose value consist of a single condition
 */
abstract class SingleConditionQuerySelector extends QuerySelector {
    @Override
    public QueryOperation getOperation(Object query) {
        if (!hasTheExpectedOperator(query)) return null;
        String fieldName = extractFieldName(query, structure());
        Object value = extractFieldValue(query, structure());
        return parseValue(fieldName, value);
    }

    protected String extractOperator(Object query) {
        return extractQueryKind(query);
    }

    protected abstract QueryOperation parseValue(String fieldName, Object value);
}