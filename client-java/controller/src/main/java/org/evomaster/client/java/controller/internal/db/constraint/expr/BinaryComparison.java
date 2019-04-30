package org.evomaster.client.java.controller.internal.db.constraint.expr;

import java.util.Objects;

public class BinaryComparison extends ConstraintExpr {
    private final ConstraintExpr leftOperand;

    private final ComparisonOperator comparisonOperator;

    private final ConstraintExpr rightOperand;

    public BinaryComparison(ConstraintExpr leftOperand, ComparisonOperator comparisonOperator, ConstraintExpr rightOperand) {
        this.leftOperand = leftOperand;
        this.comparisonOperator = comparisonOperator;
        this.rightOperand = rightOperand;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BinaryComparison that = (BinaryComparison) o;
        return Objects.equals(leftOperand, that.leftOperand) &&
                comparisonOperator == that.comparisonOperator &&
                Objects.equals(rightOperand, that.rightOperand);
    }

    @Override
    public int hashCode() {
        return Objects.hash(leftOperand, comparisonOperator, rightOperand);
    }

    @Override
    public String toString() {
        return leftOperand.toString() + " " + comparisonOperator.toString() + " " + rightOperand.toString();
    }

}
