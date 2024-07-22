package org.evomaster.solver.smtlib.value;

import java.util.Objects;

public class IntValue extends SMTLibValue {
    private final int value;

    public IntValue(int value) {
        this.value = value;
    }

    @Override
    public Integer getValue() {
        return value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IntValue intValue = (IntValue) o;
        return getValue().equals(intValue.getValue());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getValue());
    }
}