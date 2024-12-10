package org.evomaster.client.java.controller.mongo.selectors;

import org.evomaster.client.java.controller.mongo.operations.LessThanOperation;
import org.evomaster.client.java.controller.mongo.operations.QueryOperation;

/**
 * { field: { $lt: value } }
 */
public class LessThanSelector extends SingleConditionQuerySelector {

    @Override
    protected QueryOperation parseValue(String fieldName, Object value) {
        return new LessThanOperation<>(fieldName, value);
    }

    @Override
    protected String operator() {
        return "$lt";
    }
}