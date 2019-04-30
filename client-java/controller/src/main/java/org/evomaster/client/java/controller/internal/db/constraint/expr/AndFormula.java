package org.evomaster.client.java.controller.internal.db.constraint.expr;

import java.util.Objects;

public class AndFormula extends ConstraintExpr {

    private final /*non-null*/ ConstraintExpr leftFormula;

    private final /*non-null*/ ConstraintExpr rightFormula;

    public AndFormula(ConstraintExpr left, ConstraintExpr right) {
        if (left == null) {
            throw new IllegalArgumentException("Left value of AND formula cannot be null");
        }
        if (right == null) {
            throw new IllegalArgumentException("right value of AND formula cannot be null");
        }
        this.leftFormula = left;
        this.rightFormula = right;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AndFormula that = (AndFormula) o;
        return leftFormula.equals(that.leftFormula) &&
                rightFormula.equals(that.rightFormula);
    }

    @Override
    public int hashCode() {
        return Objects.hash(leftFormula, rightFormula);
    }

    @Override
    public String toString() {
        return leftFormula.toString() + " AND " + rightFormula.toString();
    }
}
