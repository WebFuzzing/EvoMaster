package org.evomaster.client.java.controller.internal.db.constraint.expr;

import java.util.Objects;

public class BooleanLiteral extends LiteralValue {

    private final /*non-null*/ boolean booleanValue;

    public BooleanLiteral(boolean booleanValue) {
        this.booleanValue = booleanValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BooleanLiteral that = (BooleanLiteral) o;
        return Objects.equals(booleanValue, that.booleanValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(booleanValue);
    }

    @Override
    public String toString() {
        return String.valueOf(booleanValue);
    }

}
