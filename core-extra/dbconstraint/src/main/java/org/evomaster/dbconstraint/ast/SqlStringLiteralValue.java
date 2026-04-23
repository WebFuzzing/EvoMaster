package org.evomaster.dbconstraint.ast;

import java.util.Objects;

public class SqlStringLiteralValue extends SqlLiteralValue {


    public String getStringValue() {
        return stringValue;
    }

    private final /*non-null*/ String stringValue;

    public SqlStringLiteralValue(String stringValue) {
        this.stringValue = Objects.requireNonNull(stringValue);
    }

    public String toSql() {
        return String.format("'%s'", stringValue);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SqlStringLiteralValue that = (SqlStringLiteralValue) o;
        return stringValue.equals(that.stringValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stringValue);
    }

    @Override
    public <K, V> K accept(SqlConditionVisitor<K, V> visitor, V argument) {
        return visitor.visit(this, argument);
    }
}
