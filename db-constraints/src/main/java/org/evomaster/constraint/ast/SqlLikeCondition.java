package org.evomaster.constraint.ast;

import java.util.Objects;

public class SqlLikeCondition extends SqlCondition {

    private final SqlColumn columnName;

    private final SqlStringLiteralValue pattern;

    public SqlLikeCondition(SqlColumn columnName, SqlStringLiteralValue pattern) {
        if (columnName == null) {
            throw new IllegalArgumentException("column name cannot be null");
        }
        if (pattern == null) {
            throw new IllegalArgumentException("pattern cannot be null");
        }
        this.columnName = columnName;
        this.pattern = pattern;
    }

    @Override
    public String toSql() {
        return String.format("%s LIKE %s", columnName.toSql(), pattern.toSql());
    }

    @Override
    public <K, V> K accept(SqlConditionVisitor<K, V> visitor, V argument) {
        return visitor.visit(this, argument);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SqlLikeCondition that = (SqlLikeCondition) o;
        return columnName.equals(that.columnName) &&
                pattern.equals(that.pattern);
    }

    @Override
    public int hashCode() {
        return Objects.hash(columnName, pattern);
    }

    public SqlColumn getColumnName() {
        return columnName;
    }

    public SqlStringLiteralValue getPattern() {
        return pattern;
    }
}
