package org.evomaster.client.java.controller.mongo.selectors;

import org.evomaster.client.java.controller.mongo.operations.*;

import java.util.ArrayList;
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
        return (value instanceof ArrayList<?>)? parseConditions((ArrayList<?>) value) : null;
    }

    @Override
    protected String extractOperator(Object query) {
        return documentKeys(query).stream().findFirst().orElse(null);
    }

    protected abstract QueryOperation parseConditions(ArrayList<?> value);
}