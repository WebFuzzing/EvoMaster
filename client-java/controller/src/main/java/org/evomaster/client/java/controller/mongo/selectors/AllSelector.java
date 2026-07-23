package org.evomaster.client.java.controller.mongo.selectors;

import org.evomaster.client.java.controller.mongo.operations.*;

import java.util.List;
import java.util.Objects;

/**
 * Represents a selector for the MongoDB `$all` operator.
 * { field: { $all: [ value1 , value2 ... ] } }
 *
 * <p>
 * This operator matches arrays that contain all elements specified in the query.
 * The selector checks if the query value is a list and, if so, creates an
 * {@link AllOperation} object corresponding to the field and the list of values.
 */
public class AllSelector extends SingleConditionQuerySelector {

    public static final String ALL_OPERATOR = "$all";

    @Override
    protected QueryOperation parseValue(String fieldName, Object value) {
        Objects.requireNonNull(fieldName);
        Objects.requireNonNull(value);

        if (value instanceof List<?>) {
            return new AllOperation<>(fieldName, (List<?>) value);
        } else {
            return null;
        }
    }

    @Override
    protected String operator() {
        return ALL_OPERATOR;
    }
}
