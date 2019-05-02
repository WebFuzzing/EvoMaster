package org.evomaster.client.java.controller.internal.db.constraint.expr;

import java.util.Objects;

public class SqlIsNotNullCondition extends SqlCondition {
    private final /*non-null*/ SqlColumnName sqlColumnName;

    public SqlIsNotNullCondition(SqlColumnName sqlColumnName) {
        if (sqlColumnName == null) {
            throw new IllegalArgumentException("Column name cannot be null");
        }
        this.sqlColumnName = sqlColumnName;
    }


    @Override
    public String toSql() {
        return sqlColumnName.toSql() + " IS NOT NULL";
    }

    @Override
    public <K, V> K accept(SqlConditionVisitor<K, V> visitor, V argument) {
        return visitor.visit(this, argument);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SqlIsNotNullCondition that = (SqlIsNotNullCondition) o;
        return sqlColumnName.equals(that.sqlColumnName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sqlColumnName);
    }
}
