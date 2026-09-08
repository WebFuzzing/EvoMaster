package org.evomaster.client.java.controller.mongo.selectors;

import org.evomaster.client.java.controller.mongo.operations.BitsAllClearOperation;
import org.evomaster.client.java.controller.mongo.operations.QueryOperation;
import org.evomaster.client.java.controller.mongo.utils.BitmaskUtils;

import java.util.Objects;
import java.util.OptionalLong;

/**
 * { field: { $bitsAllClear: value } }
 */
public class BitsAllClearSelector extends SingleConditionQuerySelector {

    public static final String BITS_ALL_CLEAR_OPERATOR = "$bitsAllClear";

    @Override
    protected QueryOperation parseValue(String fieldName, Object value) {
        Objects.requireNonNull(fieldName);
        OptionalLong bitmaskValue = BitmaskUtils.toBitMaskValue(value);
        return bitmaskValue.isPresent() ? new BitsAllClearOperation(fieldName, bitmaskValue.getAsLong()) : null;
    }

    @Override
    protected String operator() {
        return BITS_ALL_CLEAR_OPERATOR;
    }
}
