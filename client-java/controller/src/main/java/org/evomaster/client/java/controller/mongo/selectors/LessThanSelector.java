package org.evomaster.client.java.controller.mongo.selectors;

import org.evomaster.client.java.controller.mongo.operations.LessThanOperation;
import org.evomaster.client.java.controller.mongo.operations.QueryOperation;

import java.util.Objects;

/**
 * { field: { $lt: value } }
 */
public class LessThanSelector extends SingleConditionQuerySelector {

    public static final String LESS_THAN_OPERATOR = "$lt";

    @Override
    protected QueryOperation parseValue(String fieldName, Object value) {
        Objects.requireNonNull(fieldName);
        if (value==null) {
            return null;
        } else {
            return new LessThanOperation<>(fieldName, value);
        }
    }

    @Override
    protected String operator() {
        return LESS_THAN_OPERATOR;
    }
}
