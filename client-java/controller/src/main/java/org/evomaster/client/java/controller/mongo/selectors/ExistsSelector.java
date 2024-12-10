package org.evomaster.client.java.controller.mongo.selectors;

import org.evomaster.client.java.controller.mongo.operations.*;

/**
 * { field: { $exists: boolean } }
 */
public class ExistsSelector extends SingleConditionQuerySelector {
    @Override
    protected QueryOperation parseValue(String fieldName, Object value) {
        if (value instanceof Boolean) return new ExistsOperation(fieldName, (Boolean) value);
        return null;
    }

    @Override
    protected String operator() {
        return "$exists";
    }
}