package org.evomaster.client.java.sql.advanced.query_calculator.where_calculator.expressions;

import java.util.List;

public class ListExpression {

    private List<Object> values;

    public ListExpression(List<Object> stack) {
        this.values = stack;
    }

    public List<Object> getValues() {
        return values;
    }
}
