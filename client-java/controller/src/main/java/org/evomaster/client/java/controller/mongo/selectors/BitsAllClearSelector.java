package org.evomaster.client.java.controller.mongo.selectors;

import org.evomaster.client.java.controller.mongo.operations.BitsAllClearOperation;
import org.evomaster.client.java.controller.mongo.operations.QueryOperation;

import java.util.Objects;

/**
 * { field: { $bitsAllClear: value } }
 */
public class BitsAllClearSelector extends SingleConditionQuerySelector {

    public static final String BITS_ALL_CLEAR_OPERATOR = "$bitsAllClear";

    @Override
    protected QueryOperation parseValue(String fieldName, Object value) {
        Objects.requireNonNull(fieldName);
        return value instanceof Long ? new BitsAllClearOperation(fieldName, (Long) value) : null;
    }

    @Override
    protected String operator() {
        return BITS_ALL_CLEAR_OPERATOR;
    }
}
