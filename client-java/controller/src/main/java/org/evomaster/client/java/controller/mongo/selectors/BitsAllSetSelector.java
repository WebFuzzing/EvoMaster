package org.evomaster.client.java.controller.mongo.selectors;

import org.evomaster.client.java.controller.mongo.operations.BitsAllSetOperation;
import org.evomaster.client.java.controller.mongo.operations.QueryOperation;

import java.util.Objects;

/**
 * { field: { $bitsAllSet: value } }
 */
public class BitsAllSetSelector extends SingleConditionQuerySelector {

    public static final String BITS_ALL_SET_OPERATOR = "$bitsAllSet";

    @Override
    protected QueryOperation parseValue(String fieldName, Object value) {
        Objects.requireNonNull(fieldName);
        return value instanceof Long ? new BitsAllSetOperation(fieldName, (Long) value) : null;
    }

    @Override
    protected String operator() {
        return BITS_ALL_SET_OPERATOR;
    }
}
