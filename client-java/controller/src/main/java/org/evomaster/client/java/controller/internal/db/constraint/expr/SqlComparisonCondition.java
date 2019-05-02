package org.evomaster.client.java.controller.internal.db.constraint.expr;

import java.util.Objects;

public class SqlComparisonCondition extends SqlCondition {

    private final SqlCondition leftOperand;

    private final SqlComparisonOperator sqlComparisonOperator;

    private final SqlCondition rightOperand;

    public SqlComparisonCondition(SqlCondition leftOperand, SqlComparisonOperator sqlComparisonOperator, SqlCondition rightOperand) {
        this.leftOperand = leftOperand;
        this.sqlComparisonOperator = sqlComparisonOperator;
        this.rightOperand = rightOperand;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SqlComparisonCondition that = (SqlComparisonCondition) o;
        return Objects.equals(leftOperand, that.leftOperand) &&
                sqlComparisonOperator == that.sqlComparisonOperator &&
                Objects.equals(rightOperand, that.rightOperand);
    }

    @Override
    public int hashCode() {
        return Objects.hash(leftOperand, sqlComparisonOperator, rightOperand);
    }

    @Override
    public String toSql() {
        return leftOperand.toString() + " " + sqlComparisonOperator.toString() + " " + rightOperand.toString();
    }

    @Override
    public <K, V> K accept(SqlConditionVisitor<K, V> visitor, V argument) {
        return visitor.visit(this, argument);
    }

    public SqlCondition getLeftOperand() {
        return leftOperand;
    }

    public SqlComparisonOperator getSqlComparisonOperator() {
        return sqlComparisonOperator;
    }

    public SqlCondition getRightOperand() {
        return rightOperand;
    }
}
