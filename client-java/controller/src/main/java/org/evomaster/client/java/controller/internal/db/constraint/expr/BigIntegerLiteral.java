package org.evomaster.client.java.controller.internal.db.constraint.expr;

import java.math.BigInteger;
import java.util.Objects;

public class BigIntegerLiteral extends LiteralValue {

    private final /*non-null*/ BigInteger bigIntegerValue;

    public BigIntegerLiteral(int intValue) {
        this.bigIntegerValue = BigInteger.valueOf(intValue);
    }

    public BigIntegerLiteral(long longValue) {
        this.bigIntegerValue = BigInteger.valueOf(longValue);
    }


    public BigIntegerLiteral(BigInteger bigIntegerValue) {
        if (bigIntegerValue == null) {
            throw new IllegalArgumentException("cannot create big integer literal with null value");
        }
        this.bigIntegerValue = bigIntegerValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BigIntegerLiteral that = (BigIntegerLiteral) o;
        return bigIntegerValue.equals(that.bigIntegerValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bigIntegerValue);
    }

    @Override
    public String toSql() {
        return this.bigIntegerValue.toString();
    }

    @Override
    public <K, V> K accept(CheckExprVisitor<K, V> visitor, V argument) {
        return visitor.visit(this, argument);
    }

    public BigInteger getBigInteger() {
        return this.bigIntegerValue;
    }
}
