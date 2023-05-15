package org.evomaster.client.java.controller.mongo.selectors;

import org.evomaster.client.java.controller.mongo.operations.*;

import java.util.List;

/**
 * { field: { $all: [ value1 , value2 ... ] } }
 */
public class AllSelector extends SingleConditionQuerySelector {
    @Override
    protected QueryOperation parseValue(String fieldName, Object value) {
        if (value instanceof List<?>) {
            return new AllOperation<>(fieldName, (List<?>) value);
        }
        return null;
    }

    @Override
    protected String operator() {
        return "$all";
    }
}