package org.evomaster.client.java.controller.mongo.selectors;

import org.evomaster.client.java.controller.mongo.operations.*;

import java.util.List;
import java.util.Objects;

/**
 * { field: { $mod: [ divisor, remainder ] } }
 */
public class ModSelector extends SingleConditionQuerySelector {

    public static final String MOD_OPERATOR = "$mod";

    @Override
    protected QueryOperation parseValue(String fieldName, Object value) {
        Objects.requireNonNull(fieldName);
        Objects.requireNonNull(value);

        if (value instanceof List<?>) {
            List<?> listOfValues = (List<?>) value;
            Long divisor = (Long) listOfValues.get(0);
            Long remainder = (Long) listOfValues.get(1);
            return new ModOperation(fieldName, divisor, remainder);
        } else {
            return null;
        }
    }

    @Override
    protected String operator() {
        return MOD_OPERATOR;
    }
}
