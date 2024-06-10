package org.evomaster.solver.smtlib;

public class GetValue extends SMTNode {
    private final String variable;

    public GetValue(String variable) {
        this.variable = variable;
    }

    @Override
    public String toString() {
        return "(get-value (" + variable + "))";
    }
}
