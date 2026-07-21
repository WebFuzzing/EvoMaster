package org.evomaster.client.java.controller.mongo.selectors;

import org.evomaster.client.java.controller.mongo.operations.GreaterThanOperation;
import org.evomaster.client.java.controller.mongo.operations.QueryOperation;

import java.util.Objects;

/**
 * { field: { $gt: value } }
 */
public class GreaterThanSelector extends SingleConditionQuerySelector {

    public static final String GREATER_THAN_OPERATOR = "$gt";

    @Override
    protected QueryOperation parseValue(String fieldName, Object value) {
        Objects.requireNonNull(fieldName);

        if (value == null) {
            return null;
        } else {
            return new GreaterThanOperation<>(fieldName, value);
        }
    }

    @Override
    protected String operator() {
        return GREATER_THAN_OPERATOR;
    }
}
