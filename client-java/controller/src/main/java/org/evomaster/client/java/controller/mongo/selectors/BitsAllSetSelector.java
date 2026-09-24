package org.evomaster.client.java.controller.mongo.selectors;

import org.evomaster.client.java.controller.mongo.operations.BitsAllSetOperation;
import org.evomaster.client.java.controller.mongo.operations.QueryOperation;
import org.evomaster.client.java.controller.mongo.utils.BitmaskUtils;

import java.util.Objects;
import java.util.OptionalLong;

/**
 * { field: { $bitsAllSet: value } }
 */
public class BitsAllSetSelector extends SingleConditionQuerySelector {

    public static final String BITS_ALL_SET_OPERATOR = "$bitsAllSet";

    @Override
    protected QueryOperation parseValue(String fieldName, Object value) {
        Objects.requireNonNull(fieldName);
        OptionalLong bitmaskValue = BitmaskUtils.toBitMaskValue(value);
        return bitmaskValue.isPresent() ? new BitsAllSetOperation(fieldName, bitmaskValue.getAsLong()) : null;
    }

    @Override
    protected String operator() {
        return BITS_ALL_SET_OPERATOR;
    }
}
