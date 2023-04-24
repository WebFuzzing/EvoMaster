package org.evomaster.client.java.controller.mongo.selectors;

import org.evomaster.client.java.controller.mongo.operations.*;

import static org.evomaster.client.java.controller.mongo.utils.BsonHelper.getTypeFromAlias;
import static org.evomaster.client.java.controller.mongo.utils.BsonHelper.getTypeFromNumber;

/**
 * { field: { $type: BSON type } }
 */
public class TypeSelector extends SingleConditionQuerySelector {
    @Override
    protected QueryOperation parseValue(String fieldName, Object value) {
        if (value instanceof Integer) return new TypeOperation(fieldName, getTypeFromNumber((Integer) value));
        if (value instanceof String) return new TypeOperation(fieldName, getTypeFromAlias((String) value));
        return null;
    }

    @Override
    protected String operator() {
        return "$type";
    }
}