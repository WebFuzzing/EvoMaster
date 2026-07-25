package org.evomaster.client.java.controller.mongo.selectors;

import org.evomaster.client.java.controller.mongo.operations.*;
import org.evomaster.client.java.controller.mongo.QueryParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * { $nor: [ { expression1 }, { expression2 }, ...  { expressionN } ] }
 */
public class NorSelector extends MultiConditionQuerySelector {

    public static final String NOR_OPERATOR = "$nor";

    @Override
    protected QueryOperation composeConditions(List<QueryOperation> conditions) {
        Objects.requireNonNull(conditions);
        return conditions.isEmpty()? null : new NorOperation(conditions);
    }

    @Override
    protected String operator() {
        return NOR_OPERATOR;
    }
}
