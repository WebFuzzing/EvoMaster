package org.evomaster.solver.smtlib.assertion;

import java.util.List;

public class Equals extends Assertion {
    private final List<String> variables;

    public Equals(List<String> variables) {
        if (variables.size() < 2) {
            throw new IllegalArgumentException("Equals must have at least two variables");
        }
        this.variables = variables;
    }

    @Override
    public String toString() {
        return "(= " + String.join(" ", variables) + ")";
    }
}
