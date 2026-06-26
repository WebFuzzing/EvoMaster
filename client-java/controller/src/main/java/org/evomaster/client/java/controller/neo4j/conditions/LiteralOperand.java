package org.evomaster.client.java.controller.neo4j.conditions;

import java.util.Objects;

/**
 * A constant operand: a string, number, boolean or null literal.
 */
public final class LiteralOperand implements Operand {

    private final Object value;

    public LiteralOperand(Object value) {
        this.value = value;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public String toString() {
        if (value == null) {
            return "null";
        }
        return value instanceof String ? "\"" + value + "\"" : String.valueOf(value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return Objects.equals(value, ((LiteralOperand) o).value);
    }

    @Override
    public int hashCode() {
        return value != null ? value.hashCode() : 0;
    }
}
