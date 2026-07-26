package org.evomaster.client.java.controller.mongo.selectors;

import org.evomaster.client.java.controller.mongo.operations.*;
import org.evomaster.client.java.controller.mongo.QueryParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * { $or: [ { expression1 }, { expression2 }, ... , { expressionN } ] }
 */
public class OrSelector extends MultiConditionQuerySelector {

    public static final String OR_OPERATOR = "$or";

    @Override
    protected QueryOperation composeConditions(List<QueryOperation> conditions) {
        Objects.requireNonNull(conditions);
        return conditions.isEmpty()? null : new OrOperation(conditions);
    }

    @Override
    protected String operator() {
        return OR_OPERATOR;
    }
}
