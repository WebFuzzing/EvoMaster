package org.evomaster.client.java.controller.internal.db.constraint.expr;

import java.math.BigDecimal;
import java.util.Objects;

public class BigDecimalLiteral extends LiteralValue {

    private final /* non-null*/ BigDecimal bigDecimalValue;

    public BigDecimalLiteral(float floatValue) {
        this.bigDecimalValue = BigDecimal.valueOf(floatValue);
    }

    public BigDecimalLiteral(double doubleValue) {
        this.bigDecimalValue = BigDecimal.valueOf(doubleValue);
    }

    public BigDecimalLiteral(BigDecimal bigDecimalValue) {
        if (bigDecimalValue == null) {
            throw new IllegalArgumentException("cannot create a big decimal literal with a null value");
        }
        this.bigDecimalValue = bigDecimalValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BigDecimalLiteral that = (BigDecimalLiteral) o;
        return bigDecimalValue.equals(that.bigDecimalValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bigDecimalValue);
    }

    @Override
    public String toString() {
        return this.bigDecimalValue.toString();
    }

}
