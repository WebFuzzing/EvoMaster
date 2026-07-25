package org.evomaster.client.java.controller.mongo.selectors;

import org.evomaster.client.java.controller.mongo.operations.*;
import org.evomaster.client.java.controller.mongo.QueryParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 *  Represents a selector for the MongoDB `$and` operator.
 * { $and: [ { expression1 }, { expression2 } , ... , { expressionN } ] }
 */
public class AndSelector extends MultiConditionQuerySelector {

    public static final String AND_OPERATOR = "$and";

    @Override
    protected QueryOperation composeConditions(List<QueryOperation> conditions) {
        Objects.requireNonNull(conditions);
        return conditions.isEmpty() ? null : new AndOperation(conditions);
    }

    @Override
    protected String operator() {
        return AND_OPERATOR;
    }
}
