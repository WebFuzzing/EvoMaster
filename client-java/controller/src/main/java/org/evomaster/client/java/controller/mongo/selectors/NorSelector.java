package org.evomaster.client.java.controller.mongo.selectors;

import org.evomaster.client.java.controller.mongo.operations.*;
import org.evomaster.client.java.controller.mongo.QueryParser;

import java.util.ArrayList;
import java.util.List;

/**
 * { $nor: [ { <expression1> }, { <expression2> }, ...  { <expressionN> } ] }
 */
public class NorSelector extends MultiConditionQuerySelector {

    @Override
    protected QueryOperation parseConditions(ArrayList<?> value) {
        ArrayList<QueryOperation> conditions = new ArrayList<>();
        value.forEach(condition -> conditions.add(new QueryParser().parse(condition)));
        return new NorOperation(conditions);
    }

    @Override
    protected String operator() {
        return "$nor";
    }
}