package org.evomaster.client.java.controller.mongo.operations;

/**
 * Represent $where operation.
 * Matches documents that satisfy a JavaScript expression.
 */
public class WhereOperation extends QueryOperation{
    private final String expression;

    public WhereOperation(String expression) {
        this.expression = expression;
    }

    public String getExpression() {
        return expression;
    }
}