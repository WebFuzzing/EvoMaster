package org.evomaster.client.java.controller.mongo.selectors;

import org.evomaster.client.java.controller.mongo.operations.EqualsOperation;
import org.evomaster.client.java.controller.mongo.operations.QueryOperation;

import java.util.Objects;

/**
 * { field: { $eq: value } }
 */
public class EqualsSelector extends SingleConditionQuerySelector {

    public static final String EQUALS_OPERATOR = "$eq";

    /**
     * Parses the provided field name and value into a QueryOperation representing an equality condition.
     *
     * @param fieldName the name of the field involved in the condition; must not be null
     * @param value the value to match against the field; null is a valid value
     * @return an instance of EqualsOperation representing the equality condition for the given field and value
     */
    @Override
    protected QueryOperation parseValue(String fieldName, Object value) {
        Objects.requireNonNull(fieldName);
        return new EqualsOperation<>(fieldName, value);
    }

    @Override
    protected String operator() {
        return EQUALS_OPERATOR;
    }
}
