package org.evomaster.client.java.controller.mongo.selectors;

import org.evomaster.client.java.controller.mongo.operations.NotEqualsOperation;
import org.evomaster.client.java.controller.mongo.operations.QueryOperation;

import java.util.Objects;

/**
 * { field: { $ne: value } }
 */
public class NotEqualsSelector extends SingleConditionQuerySelector {

    public static final String NOT_EQUALS_OPERATOR = "$ne";

    @Override
    protected QueryOperation parseValue(String fieldName, Object value) {
        Objects.requireNonNull(fieldName);
        return new NotEqualsOperation<>(fieldName, value);
    }

    @Override
    protected String operator() {
        return NOT_EQUALS_OPERATOR;
    }
}
