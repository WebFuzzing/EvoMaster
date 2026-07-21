package org.evomaster.client.java.controller.mongo.selectors;

import org.evomaster.client.java.controller.mongo.operations.*;

import java.util.Objects;

/**
 * { field: { $exists: boolean } }
 */
public class ExistsSelector extends SingleConditionQuerySelector {

    public static final String EXISTS_OPERATOR = "$exists";

    @Override
    protected QueryOperation parseValue(String fieldName, Object value) {
        Objects.requireNonNull(fieldName);

        if (value!=null && value instanceof Boolean) {
            return new ExistsOperation(fieldName, (Boolean) value);
        } else {
            return null;
        }
    }

    @Override
    protected String operator() {
        return EXISTS_OPERATOR;
    }
}
