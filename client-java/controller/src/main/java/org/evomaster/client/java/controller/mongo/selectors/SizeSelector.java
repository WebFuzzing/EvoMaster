package org.evomaster.client.java.controller.mongo.selectors;

import org.evomaster.client.java.controller.mongo.operations.*;

/**
 * { field: { $size: value } }
 */
public class SizeSelector extends SingleConditionQuerySelector {
    @Override
    protected QueryOperation parseValue(String fieldName, Object value) {
        if (value instanceof Integer) return new SizeOperation(fieldName, (Integer) value);
        return null;
    }

    @Override
    protected String operator() {
        return "$size";
    }
}