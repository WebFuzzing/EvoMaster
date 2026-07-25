package org.evomaster.client.java.controller.mongo.selectors;

import org.evomaster.client.java.controller.mongo.operations.LessThanEqualsOperation;
import org.evomaster.client.java.controller.mongo.operations.QueryOperation;

import java.util.Objects;

/**
 * { field: { $lte: value } }
 */
public class LessThanEqualsSelector extends SingleConditionQuerySelector {

    public static final String LESS_THAN_EQUALS = "$lte";

    @Override
    protected QueryOperation parseValue(String fieldName, Object value) {
        Objects.requireNonNull(fieldName);
        if (value==null) {
            return null;
        } else {
            return new LessThanEqualsOperation<>(fieldName, value);
        }
    }

    @Override
    protected String operator() {
        return LESS_THAN_EQUALS;
    }
}
