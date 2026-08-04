package org.evomaster.client.java.controller.mongo.selectors;

import org.evomaster.client.java.controller.mongo.operations.*;
import org.evomaster.client.java.controller.mongo.QueryParser;

import java.util.Set;

import static org.evomaster.client.java.controller.mongo.selectors.AndSelector.AND_OPERATOR;
import static org.evomaster.client.java.controller.mongo.selectors.OrSelector.OR_OPERATOR;
import static org.evomaster.client.java.controller.mongo.selectors.NorSelector.NOR_OPERATOR;

import static org.evomaster.client.java.controller.mongo.utils.BsonHelper.*;

/**
 * { field: { $not: { operator-expression } } }
 */
public class NotSelector extends SingleConditionQuerySelector {

    public static final String NOT_OPERATOR = "$not";

    @Override
    protected QueryOperation parseValue(String fieldName, Object value) {
        if (isBsonDocument(value)) {
            // A $not cannot contain $and, $or, $nor, etc. (logical operators at field level)
            // or other $not
            Set<String> keys = documentKeys(value);
            if (keys == null || keys.isEmpty()) {
                return null;
            }
            String innerOp = keys.iterator().next();
            if (innerOp.equals(NOT_OPERATOR)
                    || innerOp.equals(AND_OPERATOR)
                    || innerOp.equals(OR_OPERATOR)
                    || innerOp.equals(NOR_OPERATOR)) {
                return null;
            }

            // This is necessary for query parser to work correctly as the syntax for not is different
            // The field is at the beginning instead
            Object docWithRemovedNot = newDocument(value);
            appendToDocument(docWithRemovedNot, fieldName, value);
            QueryOperation condition = new QueryParser().parse(docWithRemovedNot);

            if (condition == null || condition instanceof AndOperation) {
                // If it parsed as AndOperation (because of ImplicitSelector), it's probably wrong here
                // or if it failed to parse.
                return null;
            } else {
                return new NotOperation(fieldName, condition);
            }
        } else {
            return null;
        }
    }

    @Override
    protected String operator() {
        return NOT_OPERATOR;
    }
}
