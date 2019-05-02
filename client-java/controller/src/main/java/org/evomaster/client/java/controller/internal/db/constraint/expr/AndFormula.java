package org.evomaster.client.java.controller.internal.db.constraint.expr;

import java.util.Objects;

public class AndFormula extends CheckExpr {

    private final /*non-null*/ CheckExpr leftExpr;

    private final /*non-null*/ CheckExpr rightExpr;

    public AndFormula(CheckExpr left, CheckExpr right) {
        if (left == null) {
            throw new IllegalArgumentException("Left value of AND formula cannot be null");
        }
        if (right == null) {
            throw new IllegalArgumentException("right value of AND formula cannot be null");
        }
        this.leftExpr = left;
        this.rightExpr = right;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AndFormula that = (AndFormula) o;
        return leftExpr.equals(that.leftExpr) &&
                rightExpr.equals(that.rightExpr);
    }

    public String toSql() {
        return leftExpr.toSql() + " AND " + rightExpr.toSql();
    }

    @Override
    public int hashCode() {
        return Objects.hash(leftExpr, rightExpr);
    }

    @Override
    public <K, V> K accept(CheckExprVisitor<K, V> visitor, V argument) {
        return visitor.visit(this, argument);
    }

    public CheckExpr getLeftExpr() {
        return leftExpr;
    }

    public CheckExpr getRightExpr() {
        return rightExpr;
    }
}
