package org.evomaster.dbconstraint.ast;

import java.util.Objects;

public class SqlSimilarToCondition extends SqlCondition {

    private final /*non-null*/ SqlColumn column;

    private final /*non-null*/ SqlStringLiteralValue pattern;


    public SqlSimilarToCondition(SqlColumn column, SqlStringLiteralValue pattern) {
        this.column = Objects.requireNonNull(column);
        this.pattern = Objects.requireNonNull(pattern);
    }

    @Override
    public String toSql() {
        return String.format("%s SIMILAR TO %s", column.toSql(), pattern.toSql());
    }

    @Override
    public <K, V> K accept(SqlConditionVisitor<K, V> visitor, V argument) {
        return visitor.visit(this, argument);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SqlSimilarToCondition that = (SqlSimilarToCondition) o;
        return column.equals(that.column) &&
                pattern.equals(that.pattern);
    }

    @Override
    public int hashCode() {
        return Objects.hash(column, pattern);
    }

    public SqlColumn getColumn() {
        return column;
    }

    public SqlStringLiteralValue getPattern() {
        return pattern;
    }
}
