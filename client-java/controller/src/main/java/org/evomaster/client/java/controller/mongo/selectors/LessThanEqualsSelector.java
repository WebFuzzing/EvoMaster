package org.evomaster.client.java.controller.mongo.selectors;

import org.evomaster.client.java.controller.mongo.operations.LessThanEqualsOperation;
import org.evomaster.client.java.controller.mongo.operations.QueryOperation;

/**
 * { field: { $lte: value } }
 */
public class LessThanEqualsSelector extends SingleConditionQuerySelector {

    @Override
    protected QueryOperation parseValue(String fieldName, Object value) {
        return new LessThanEqualsOperation<>(fieldName, value);
    }

    @Override
    protected String operator() {
        return "$lte";
    }
}