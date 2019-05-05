package org.evomaster.dbconstraint.ast;

import java.util.Objects;

public class SqlBooleanLiteralValue extends SqlLiteralValue {

    private final /*non-null*/ boolean booleanValue;

    public SqlBooleanLiteralValue(boolean booleanValue) {
        this.booleanValue = booleanValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SqlBooleanLiteralValue that = (SqlBooleanLiteralValue) o;
        return Objects.equals(booleanValue, that.booleanValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(booleanValue);
    }

    @Override
    public String toSql() {
        return String.valueOf(booleanValue);
    }

    @Override
    public <K, V> K accept(SqlConditionVisitor<K, V> visitor, V argument) {
        return visitor.visit(this, argument);
    }
}
