package org.evomaster.client.java.controller.mongo.selectors;

import org.evomaster.client.java.controller.mongo.operations.BitsAnyClearOperation;
import org.evomaster.client.java.controller.mongo.operations.QueryOperation;
import org.evomaster.client.java.controller.mongo.utils.BitmaskUtils;

import java.util.Objects;
import java.util.OptionalLong;

/**
 * { field: { $bitsAnyClear: value } }
 */
public class BitsAnyClearSelector extends SingleConditionQuerySelector {

    public static final String BITS_ANY_CLEAR_OPERATOR = "$bitsAnyClear";

    @Override
    protected QueryOperation parseValue(String fieldName, Object value) {
        Objects.requireNonNull(fieldName);
        OptionalLong bitmaskValue = BitmaskUtils.toBitMaskValue(value);
        return bitmaskValue.isPresent() ? new BitsAnyClearOperation(fieldName, bitmaskValue.getAsLong()) : null;
    }

    @Override
    protected String operator() {
        return BITS_ANY_CLEAR_OPERATOR;
    }
}
