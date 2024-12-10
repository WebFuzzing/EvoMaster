package org.evomaster.client.java.controller.mongo.selectors;

import org.evomaster.client.java.controller.mongo.operations.GreaterThanEqualsOperation;
import org.evomaster.client.java.controller.mongo.operations.QueryOperation;

/**
 * { field: { $gte: value } }
 */
public class GreaterThanEqualsSelector extends SingleConditionQuerySelector {

    @Override
    protected QueryOperation parseValue(String fieldName, Object value) {
        return new GreaterThanEqualsOperation<>(fieldName, value);
    }

    @Override
    protected String operator() {
        return "$gte";
    }
}