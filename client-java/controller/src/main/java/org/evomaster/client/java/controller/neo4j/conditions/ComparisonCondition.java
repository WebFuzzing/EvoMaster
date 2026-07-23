package org.evomaster.client.java.controller.neo4j.conditions;

import java.util.Objects;

/**
 * A comparison from a WHERE clause: left OP right.
 * Both sides are typed {@link Operand}s so each can be valuated against the graph
 * independently. The right operand is null for the unary IS NULL / IS NOT NULL operators.
 */
public class ComparisonCondition implements CypherCondition {

    private final Operand left;
    private final ComparisonOperator operator;
    private final Operand right;

    public ComparisonCondition(Operand left, ComparisonOperator operator, Operand right) {
        this.left = Objects.requireNonNull(left, "left operand must not be null");
        this.operator = Objects.requireNonNull(operator, "operator must not be null");
        this.right = right;
    }

    public Operand getLeft() {
        return left;
    }

    public ComparisonOperator getOperator() {
        return operator;
    }

    public Operand getRight() {
        return right;
    }

    @Override
    public <T> T accept(CypherConditionVisitor<T> visitor) {
        return visitor.visitComparison(this);
    }

    @Override
    public String toString() {
        if (right == null) {
            return left + " " + operator.getSymbol();
        }
        return left + " " + operator.getSymbol() + " " + right;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ComparisonCondition that = (ComparisonCondition) o;
        return operator == that.operator
                && Objects.equals(left, that.left)
                && Objects.equals(right, that.right);
    }

    @Override
    public int hashCode() {
        int result = left != null ? left.hashCode() : 0;
        result = 31 * result + (operator != null ? operator.hashCode() : 0);
        result = 31 * result + (right != null ? right.hashCode() : 0);
        return result;
    }
}
