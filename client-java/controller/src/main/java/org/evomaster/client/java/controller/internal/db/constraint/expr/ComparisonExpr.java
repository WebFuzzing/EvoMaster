package org.evomaster.client.java.controller.internal.db.constraint.expr;

import java.util.Objects;

public class ComparisonExpr extends CheckExpr {

    private final CheckExpr leftOperand;

    private final ComparisonOperator comparisonOperator;

    private final CheckExpr rightOperand;

    public ComparisonExpr(CheckExpr leftOperand, ComparisonOperator comparisonOperator, CheckExpr rightOperand) {
        this.leftOperand = leftOperand;
        this.comparisonOperator = comparisonOperator;
        this.rightOperand = rightOperand;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ComparisonExpr that = (ComparisonExpr) o;
        return Objects.equals(leftOperand, that.leftOperand) &&
                comparisonOperator == that.comparisonOperator &&
                Objects.equals(rightOperand, that.rightOperand);
    }

    @Override
    public int hashCode() {
        return Objects.hash(leftOperand, comparisonOperator, rightOperand);
    }

    @Override
    public String toSql() {
        return leftOperand.toString() + " " + comparisonOperator.toString() + " " + rightOperand.toString();
    }

    @Override
    public <K, V> K accept(CheckExprVisitor<K, V> visitor, V argument) {
        return visitor.visit(this, argument);
    }

    public CheckExpr getLeftOperand() {
        return leftOperand;
    }

    public ComparisonOperator getComparisonOperator() {
        return comparisonOperator;
    }

    public CheckExpr getRightOperand() {
        return rightOperand;
    }
}
