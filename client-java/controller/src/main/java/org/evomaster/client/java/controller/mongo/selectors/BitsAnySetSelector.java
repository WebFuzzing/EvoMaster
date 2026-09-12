package org.evomaster.client.java.controller.mongo.selectors;

import org.evomaster.client.java.controller.mongo.operations.BitsAnySetOperation;
import org.evomaster.client.java.controller.mongo.operations.QueryOperation;
import org.evomaster.client.java.controller.mongo.utils.BitmaskUtils;

import java.util.List;
import java.util.Objects;
import java.util.OptionalLong;

/**
 * { field: { $bitsAnySet: value } }
 */
public class BitsAnySetSelector extends SingleConditionQuerySelector {

    public static final String BITS_ANY_SET_OPERATOR = "$bitsAnySet";

    @Override
    protected QueryOperation parseValue(String fieldName, Object value) {
        Objects.requireNonNull(fieldName);
        OptionalLong bitmaskValue = BitmaskUtils.toBitMaskValue(value);
        return bitmaskValue.isPresent() ? new BitsAnySetOperation(fieldName, bitmaskValue.getAsLong()) : null;
    }


    @Override
    protected String operator() {
        return BITS_ANY_SET_OPERATOR;
    }
}
