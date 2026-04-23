package org.evomaster.solver.smtlib;

public class GetValueSMTNode extends SMTNode {
    private final String variable;

    public GetValueSMTNode(String variable) {
        this.variable = variable;
    }

    @Override
    public String toString() {
        return "(get-value (" + variable + "))";
    }
}
