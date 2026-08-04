package org.evomaster.client.java.controller.mongo.selectors;

import org.evomaster.client.java.controller.mongo.operations.*;

import java.util.Objects;

/**
 * { field: { $size: value } }
 */
public class SizeSelector extends SingleConditionQuerySelector {

    public static final String SIZE_OPERATOR = "$size";

    @Override
    protected QueryOperation parseValue(String fieldName, Object value) {
        Objects.requireNonNull(fieldName);
        Objects.requireNonNull(value);

        if (value instanceof Integer) {
            return new SizeOperation(fieldName, (Integer) value);
        } else {
            return null;
        }
    }

    @Override
    protected String operator() {
        return SIZE_OPERATOR;
    }
}
