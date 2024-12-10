package org.evomaster.client.java.controller.mongo.selectors;

import org.evomaster.client.java.controller.mongo.operations.*;

import java.util.Map;
import java.util.Set;

import static org.evomaster.client.java.controller.mongo.utils.BsonHelper.*;

/**
 * Selectors for operations whose value consist of a single condition
 */
abstract class SingleConditionQuerySelector extends QuerySelector {
    @Override
    public QueryOperation getOperation(Object query) {
        String fieldName = extractFieldName(query);
        Object innerDoc = getValue(query, fieldName);
        if (!isUniqueEntry((Map<?, ?>) query) || !isDocument(innerDoc) || !hasTheExpectedOperator(query)) return null;
        Object value = getValue(innerDoc, operator());
        return parseValue(fieldName, value);
    }

    protected String extractOperator(Object query) {
        String fieldName = extractFieldName(query);
        Set<String> keys = documentKeys(getValue(query, fieldName));
        return keys.stream().findFirst().orElse(null);
    }

    protected abstract QueryOperation parseValue(String fieldName, Object value);

    private String extractFieldName(Object query) {
        Set<String> keys = documentKeys(query);
        return keys.stream().findFirst().orElse(null);
    }
}