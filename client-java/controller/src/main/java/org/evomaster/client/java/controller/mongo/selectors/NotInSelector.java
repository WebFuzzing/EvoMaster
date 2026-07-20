package org.evomaster.client.java.controller.mongo.selectors;

import org.evomaster.client.java.controller.mongo.operations.*;

import java.util.List;
import java.util.Objects;

/**
 * { field: { $nin: [ value1, value2 ... valueN ] } }
 */
public class NotInSelector extends SingleConditionQuerySelector {

    public static final String NOT_IN_OPERATOR = "$nin";

    @Override
    protected QueryOperation parseValue(String fieldName, Object value) {
        Objects.requireNonNull(fieldName);
        Objects.requireNonNull(value);

        if (value instanceof List<?>) {
            return new NotInOperation<>(fieldName, (List<?>) value);
        } else {
            return null;
        }
    }

    @Override
    protected String operator() {
        return NOT_IN_OPERATOR;
    }
}
