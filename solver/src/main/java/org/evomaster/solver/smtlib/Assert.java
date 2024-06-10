package org.evomaster.solver.smtlib;

public class Assert extends SMTNode {
    private final String condition;

    public Assert(String condition) {
        this.condition = condition;
    }

    @Override
    public String toString() {
        return "(assert " + condition + ")";
    }
}
