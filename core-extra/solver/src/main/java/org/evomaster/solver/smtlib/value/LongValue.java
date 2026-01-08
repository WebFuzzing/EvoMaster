package org.evomaster.solver.smtlib.value;

import java.util.Objects;

public class LongValue extends SMTLibValue {
    private final Long value;

    public LongValue(Long value) {
        this.value = value;
    }

    @Override
    public Long getValue() {
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
        LongValue longValue = (LongValue) o;
        return getValue().equals(longValue.getValue());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getValue());
    }
}