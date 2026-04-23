package org.evomaster.dbconstraint.ast;

import java.math.BigDecimal;
import java.util.Objects;

public class SqlBigDecimalLiteralValue extends SqlLiteralValue {

    private final /* non-null*/ BigDecimal bigDecimalValue;

    public SqlBigDecimalLiteralValue(float floatValue) {
        this.bigDecimalValue = BigDecimal.valueOf(floatValue);
    }

    public SqlBigDecimalLiteralValue(double doubleValue) {
        this.bigDecimalValue = BigDecimal.valueOf(doubleValue);
    }

    public SqlBigDecimalLiteralValue(BigDecimal bigDecimalValue) {
        this.bigDecimalValue = Objects.requireNonNull(bigDecimalValue);
    }

    public BigDecimal getBigDecimal() {
        return this.bigDecimalValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SqlBigDecimalLiteralValue that = (SqlBigDecimalLiteralValue) o;
        return bigDecimalValue.equals(that.bigDecimalValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bigDecimalValue);
    }

    @Override
    public String toSql() {
        return this.bigDecimalValue.toString();
    }

    @Override
    public <K, V> K accept(SqlConditionVisitor<K, V> visitor, V argument) {
        return visitor.visit(this, argument);
    }
}
