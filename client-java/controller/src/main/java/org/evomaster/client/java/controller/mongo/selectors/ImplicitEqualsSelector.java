package org.evomaster.client.java.controller.mongo.selectors;

import org.evomaster.client.java.controller.mongo.operations.*;

import static org.evomaster.client.java.controller.mongo.utils.BsonHelper.getValue;
import static org.evomaster.client.java.controller.mongo.utils.BsonHelper.documentKeys;

import java.util.Map;
import java.util.Set;

/**
 * { field: value }
 */
public class ImplicitEqualsSelector extends QuerySelector {
    @Override
    public QueryOperation getOperation(Object query) {
        String fieldName = extractFieldName(query);
        if (!isUniqueEntry((Map<?, ?>) query)) return null;
        Object value = getValue(query, fieldName);
        return new EqualsOperation<>(fieldName, value);
    }

    protected String extractOperator(Object query) {
        String fieldName = extractFieldName(query);
        Set<String> keys = documentKeys(getValue(query, fieldName));
        return keys.stream().findFirst().orElse(null);
    }

    @Override
    protected String operator() {
        return "";
    }

    private String extractFieldName(Object query) {
        Set<String> keys = documentKeys(query);
        return keys.stream().findFirst().orElse(null);
    }
}