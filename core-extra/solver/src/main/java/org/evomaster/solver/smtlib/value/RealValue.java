package org.evomaster.solver.smtlib.value;

import java.util.Objects;

public class RealValue extends SMTLibValue {
    private final double value;

    public RealValue(double value) {
        this.value = value;
    }

    @Override
    public Double getValue() {
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
        RealValue realValue = (RealValue) o;
        return Double.compare(getValue(), realValue.getValue()) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getValue());
    }
}
