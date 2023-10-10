package org.evomaster.client.java.controller.mongo.selectors;

import org.evomaster.client.java.controller.mongo.operations.*;
import org.evomaster.client.java.controller.mongo.QueryParser;

import static org.evomaster.client.java.controller.mongo.utils.BsonHelper.*;

/**
 * { field: { $not: { operator-expression } } }
 */
public class NotSelector extends SingleConditionQuerySelector {
    @Override
    protected QueryOperation parseValue(String fieldName, Object value) {
        if (isDocument(value)) {
            // This is necessary for query parser to work correctly as the syntax for not is different
            // The field is at the beginning instead
            Object docWithRemovedNot = newDocument(value);
            appendToDocument(docWithRemovedNot, fieldName, value);
            QueryOperation condition = new QueryParser().parse(docWithRemovedNot);
            return new NotOperation(fieldName, condition);
        }
        return null;
    }

    @Override
    protected String operator() {
        return "$not";
    }
}