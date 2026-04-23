package org.evomaster.dbconstraint.ast;

import java.util.Objects;

public class SqlIsNullCondition extends SqlCondition {

    private final /*non-null*/ SqlColumn columnName;

    public SqlIsNullCondition(SqlColumn columnName) {
        this.columnName = Objects.requireNonNull(columnName);
    }

    @Override
    public String toSql() {
        return String.format("%s IS NULL", columnName.toSql());
    }

    @Override
    public <K, V> K accept(SqlConditionVisitor<K, V> visitor, V argument) {
        return visitor.visit(this, argument);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SqlIsNullCondition that = (SqlIsNullCondition) o;
        return columnName.equals(that.columnName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(columnName);
    }
}
