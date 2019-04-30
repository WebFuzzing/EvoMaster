package org.evomaster.client.java.controller.internal.db.constraint.expr;

public enum ComparisonOperator {
    EQUALS_TO("="),
    GREATER_THAN(">"),
    GREATER_THAN_OR_EQUAL(">="),
    LESS_THAN("<"),
    LESS_THAN_OR_EQUAL("<=");

    private String name;

    ComparisonOperator(String name) {
        this.name = name;
    }

    public String toString() {
        return this.name;
    }

}
