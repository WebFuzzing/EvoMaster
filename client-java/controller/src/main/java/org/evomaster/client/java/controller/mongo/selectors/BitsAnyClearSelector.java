package org.evomaster.client.java.controller.mongo.selectors;

import org.evomaster.client.java.controller.mongo.operations.BitsAnyClearOperation;
import org.evomaster.client.java.controller.mongo.operations.QueryOperation;

import java.util.Objects;

/**
 * { field: { $bitsAnyClear: value } }
 */
public class BitsAnyClearSelector extends SingleConditionQuerySelector {

    public static final String BITS_ANY_CLEAR_OPERATOR = "$bitsAnyClear";

    @Override
    protected QueryOperation parseValue(String fieldName, Object value) {
        Objects.requireNonNull(fieldName);
        return value instanceof Long ? new BitsAnyClearOperation(fieldName, (Long) value) : null;
    }

    @Override
    protected String operator() {
        return BITS_ANY_CLEAR_OPERATOR;
    }
}
