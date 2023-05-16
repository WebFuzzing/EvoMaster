package org.evomaster.client.java.controller.mongo.selectors;

import org.evomaster.client.java.controller.mongo.operations.*;

import java.util.List;

/**
 * { field: { $mod: [ divisor, remainder ] } }
 */
public class ModSelector extends SingleConditionQuerySelector {
    @Override
    protected QueryOperation parseValue(String fieldName, Object value) {
        if (value instanceof List<?>) {
            Long divisor = (Long) ((List<?>) value).get(0);
            Long remainder = (Long) ((List<?>) value).get(1);
            return new ModOperation(fieldName, divisor, remainder);
        }
        return null;
    }

    @Override
    protected String operator() {
        return "$mod";
    }
}