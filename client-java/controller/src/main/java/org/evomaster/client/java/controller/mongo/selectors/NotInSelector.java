package org.evomaster.client.java.controller.mongo.selectors;

import org.evomaster.client.java.controller.mongo.operations.*;

import java.util.List;

/**
 * { field: { $nin: [ value1, value2 ... valueN ] } }
 */
public class NotInSelector extends SingleConditionQuerySelector {
    @Override
    protected QueryOperation parseValue(String fieldName, Object value) {
        if (value instanceof List<?>) return new NotInOperation<>(fieldName, (List<?>) value);
        return null;
    }

    @Override
    protected String operator() {
        return "$nin";
    }
}