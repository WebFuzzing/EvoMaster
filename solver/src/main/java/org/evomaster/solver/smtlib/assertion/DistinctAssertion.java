package org.evomaster.solver.smtlib.assertion;

import java.util.List;

public class DistinctAssertion extends Assertion {
    private final List<String> variables;

    public DistinctAssertion(List<String> variables) {
        if (variables.size() < 2) {
            throw new IllegalArgumentException("Distinct must have at least two variables");
        }
        this.variables = variables;
    }

    @Override
    public String toString() {
        return "(distinct " + String.join(" ", variables) + ")";
    }
}