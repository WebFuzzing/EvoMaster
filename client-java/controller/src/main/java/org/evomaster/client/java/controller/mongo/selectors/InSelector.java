package org.evomaster.client.java.controller.mongo.selectors;

import org.evomaster.client.java.controller.mongo.operations.*;

import java.util.List;
import java.util.Objects;

/**
 * { field: { $in: [value1, value2, ... valueN ] } }
 */
public class InSelector extends SingleConditionQuerySelector {

    public static final String IN_OPERATOR = "$in";

    @Override
    protected QueryOperation parseValue(String fieldName, Object value) {
        Objects.requireNonNull(fieldName);
        Objects.requireNonNull(value);

        if (value instanceof List<?>) {
            return new InOperation<>(fieldName, (List<?>) value);
        } else {
            return null;
        }
    }

    @Override
    protected String operator() {
        return IN_OPERATOR;
    }
}
