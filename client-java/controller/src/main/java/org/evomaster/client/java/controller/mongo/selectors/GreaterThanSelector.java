package org.evomaster.client.java.controller.mongo.selectors;

import org.evomaster.client.java.controller.mongo.operations.GreaterThanOperation;
import org.evomaster.client.java.controller.mongo.operations.QueryOperation;

/**
 * { field: { $gt: value } }
 */
public class GreaterThanSelector extends SingleConditionQuerySelector {

    @Override
    protected QueryOperation parseValue(String fieldName, Object value) {
        return new GreaterThanOperation<>(fieldName, value);
    }

    @Override
    protected String operator() {
        return "$gt";
    }
}