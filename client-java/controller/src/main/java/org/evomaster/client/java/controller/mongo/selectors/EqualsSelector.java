package org.evomaster.client.java.controller.mongo.selectors;

import org.evomaster.client.java.controller.mongo.operations.EqualsOperation;
import org.evomaster.client.java.controller.mongo.operations.QueryOperation;

/**
 * { field: { $eq: value } }
 */
public class EqualsSelector extends SingleConditionQuerySelector {

    @Override
    protected QueryOperation parseValue(String fieldName, Object value) {
        return new EqualsOperation<>(fieldName, value);
    }

    @Override
    protected String operator() {
        return "$eq";
    }
}