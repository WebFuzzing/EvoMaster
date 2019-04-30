package org.evomaster.client.java.controller.internal.db.constraint.expr;

import java.util.Objects;

public class StringLiteral extends LiteralValue {


    private final /*non-null*/ String stringValue;

    public StringLiteral(String stringValue) {
        if (stringValue == null) {
            throw new IllegalArgumentException("value cannot be null");
        }
        this.stringValue = stringValue;
    }

    @Override
    public String toString() {
        return stringValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StringLiteral that = (StringLiteral) o;
        return stringValue.equals(that.stringValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stringValue);
    }
}
