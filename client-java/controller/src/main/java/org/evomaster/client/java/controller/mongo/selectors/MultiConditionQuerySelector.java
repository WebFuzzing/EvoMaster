package org.evomaster.client.java.controller.mongo.selectors;

import org.evomaster.client.java.controller.mongo.operations.*;

import java.util.List;
import java.util.Map;

import static org.evomaster.client.java.controller.mongo.utils.BsonHelper.getValue;
import static org.evomaster.client.java.controller.mongo.utils.BsonHelper.documentKeys;

/**
 * Selectors for operations whose value consist of a list of conditions
 */
abstract public class MultiConditionQuerySelector extends QuerySelector {
    @Override
    public QueryOperation getOperation(Object query) {
        if (!isUniqueEntry((Map<?, ?>) query) || !hasTheExpectedOperator(query)) return null;
        Object value = getValue(query, operator());
        return (value instanceof List<?>)? parseConditions((List<?>) value) : null;
    }

    @Override
    protected String extractOperator(Object query) {
        return documentKeys(query).stream().findFirst().orElse(null);
    }

    protected abstract QueryOperation parseConditions(List<?> value);
}