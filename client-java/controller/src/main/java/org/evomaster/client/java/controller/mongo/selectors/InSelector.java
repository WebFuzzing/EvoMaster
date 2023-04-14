package org.evomaster.client.java.controller.mongo.selectors;

import org.evomaster.client.java.controller.mongo.operations.*;

import java.util.ArrayList;

/**
 * { field: { $in: [<value1>, <value2>, ... <valueN> ] } }
 */
public class InSelector extends SingleConditionQuerySelector {
    @Override
    protected QueryOperation parseValue(String fieldName, Object value) {
        if (value instanceof ArrayList<?>) return new InOperation<>(fieldName, (ArrayList<?>) value);
        return null;
    }

    @Override
    protected String operator() {
        return "$in";
    }
}