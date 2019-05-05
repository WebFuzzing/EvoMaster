package org.evomaster.dbconstraint.ast;

import java.util.Objects;

public class SqlAndCondition extends SqlCondition {

    private final /*non-null*/ SqlCondition leftExpr;

    private final /*non-null*/ SqlCondition rightExpr;

    public SqlAndCondition(SqlCondition left, SqlCondition right) {
        this.leftExpr = Objects.requireNonNull(left);
        this.rightExpr = Objects.requireNonNull(right);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SqlAndCondition that = (SqlAndCondition) o;
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
    public <K, V> K accept(SqlConditionVisitor<K, V> visitor, V argument) {
        return visitor.visit(this, argument);
    }

    public SqlCondition getLeftExpr() {
        return leftExpr;
    }

    public SqlCondition getRightExpr() {
        return rightExpr;
    }
}
