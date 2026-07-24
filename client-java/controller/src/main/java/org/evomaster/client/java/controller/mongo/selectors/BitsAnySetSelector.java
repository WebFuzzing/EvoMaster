package org.evomaster.client.java.controller.mongo.selectors;

import org.evomaster.client.java.controller.mongo.operations.BitsAnySetOperation;
import org.evomaster.client.java.controller.mongo.operations.QueryOperation;

import java.util.Objects;

/**
 * { field: { $bitsAnySet: value } }
 */
public class BitsAnySetSelector extends SingleConditionQuerySelector {

    public static final String BITS_ANY_SET_OPERATOR = "$bitsAnySet";

    @Override
    protected QueryOperation parseValue(String fieldName, Object value) {
        Objects.requireNonNull(fieldName);
        return value instanceof Long ? new BitsAnySetOperation(fieldName, (Long) value) : null;
    }

    @Override
    protected String operator() {
        return BITS_ANY_SET_OPERATOR;
    }
}
