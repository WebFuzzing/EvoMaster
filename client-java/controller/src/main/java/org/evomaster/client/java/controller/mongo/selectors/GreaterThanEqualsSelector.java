package org.evomaster.client.java.controller.mongo.selectors;

import org.evomaster.client.java.controller.mongo.operations.GreaterThanEqualsOperation;
import org.evomaster.client.java.controller.mongo.operations.QueryOperation;

import java.util.Objects;

/**
 * { field: { $gte: value } }
 */
public class GreaterThanEqualsSelector extends SingleConditionQuerySelector {

    public static final String GREATER_THAN_EQUALS_OPERATOR = "$gte";

    @Override
    protected QueryOperation parseValue(String fieldName, Object value) {
        Objects.requireNonNull(fieldName);
        if (value==null) {
            return null;
        } else {
            return new GreaterThanEqualsOperation<>(fieldName, value);
        }
    }

    @Override
    protected String operator() {
        return GREATER_THAN_EQUALS_OPERATOR;
    }
}
