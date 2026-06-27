package org.evomaster.client.java.controller.neo4j.conditions;

import java.util.Objects;

/**
 * A WHERE predicate kept unchanged because the structural model does not decompose
 * it (e.g. a boolean-returning function call or a bare boolean expression). It exists
 * so that no part of the query is silently dropped: the boolean structure around it
 * (AND/OR/XOR/NOT) is still preserved, and this leaf carries the original text.
 */
public class RawCondition implements CypherCondition {

    private final String expression;

    public RawCondition(String expression) {
        this.expression = Objects.requireNonNull(expression, "expression must not be null");
    }

    public String getExpression() {
        return expression;
    }

    @Override
    public String toString() {
        return expression;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return Objects.equals(expression, ((RawCondition) o).expression);
    }

    @Override
    public int hashCode() {
        return expression != null ? expression.hashCode() : 0;
    }
}
