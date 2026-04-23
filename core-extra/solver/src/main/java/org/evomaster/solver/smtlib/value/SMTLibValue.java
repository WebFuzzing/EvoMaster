package org.evomaster.solver.smtlib.value;

// Abstract base class for different types of values: Int, Real, String or composed type
public abstract class SMTLibValue {
    public abstract Object getValue();
}