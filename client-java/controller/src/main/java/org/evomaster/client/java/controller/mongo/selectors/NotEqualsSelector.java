package org.evomaster.client.java.controller.mongo.selectors;

import org.evomaster.client.java.controller.mongo.operations.NotEqualsOperation;
import org.evomaster.client.java.controller.mongo.operations.QueryOperation;

/**
 * { field: { $ne: value } }
 */
public class NotEqualsSelector extends SingleConditionQuerySelector {

    @Override
    protected QueryOperation parseValue(String fieldName, Object value) {
        return new NotEqualsOperation<>(fieldName, value);
    }

    @Override
    protected String operator() {
        return "$ne";
    }
}