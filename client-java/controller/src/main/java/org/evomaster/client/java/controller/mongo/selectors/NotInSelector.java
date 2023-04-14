package org.evomaster.client.java.controller.mongo.selectors;

import org.evomaster.client.java.controller.mongo.operations.*;

import java.util.ArrayList;

/**
 * { field: { $nin: [ <value1>, <value2> ... <valueN> ] } }
 */
public class NotInSelector extends SingleConditionQuerySelector {
    @Override
    protected QueryOperation parseValue(String fieldName, Object value) {
        if (value instanceof ArrayList<?>) return new NotInOperation<>(fieldName, (ArrayList<?>) value);
        return null;
    }

    @Override
    protected String operator() {
        return "$nin";
    }
}