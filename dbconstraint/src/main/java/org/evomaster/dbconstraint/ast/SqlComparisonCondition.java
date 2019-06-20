package org.evomaster.dbconstraint.ast;

import java.util.Objects;

public class SqlComparisonCondition extends SqlCondition {

    private final /*non-null*/ SqlCondition leftOperand;

    private final /*non-null*/ SqlComparisonOperator sqlComparisonOperator;

    private final /*non-null*/ SqlCondition rightOperand;

    public SqlComparisonCondition(SqlCondition leftOperand, SqlComparisonOperator sqlComparisonOperator, SqlCondition rightOperand) {
        this.leftOperand = Objects.requireNonNull(leftOperand);
        this.sqlComparisonOperator = Objects.requireNonNull(sqlComparisonOperator);
        this.rightOperand = Objects.requireNonNull(rightOperand);
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
