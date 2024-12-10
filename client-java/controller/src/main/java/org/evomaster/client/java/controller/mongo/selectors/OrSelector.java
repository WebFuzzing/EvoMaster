package org.evomaster.client.java.controller.mongo.selectors;

import org.evomaster.client.java.controller.mongo.operations.*;
import org.evomaster.client.java.controller.mongo.QueryParser;

import java.util.ArrayList;
import java.util.List;

/**
 * { $or: [ { expression1 }, { expression2 }, ... , { expressionN } ] }
 */
public class OrSelector extends MultiConditionQuerySelector {
    @Override
    protected QueryOperation parseConditions(List<?> value) {
        ArrayList<QueryOperation> conditions = new ArrayList<>();
        value.forEach(condition -> conditions.add(new QueryParser().parse(condition)));
        return conditions.isEmpty()? null : new OrOperation(conditions);
    }

    @Override
    protected String operator() {
        return "$or";
    }
}