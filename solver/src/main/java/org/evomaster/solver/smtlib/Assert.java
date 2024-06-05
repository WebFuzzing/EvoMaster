package org.evomaster.solver.smtlib;

import org.evomaster.solver.smtlib.assertion.Assertion;

public class Assert extends SMTNode {
    private final Assertion assertion;

    public Assert(Assertion assertion) {
        this.assertion = assertion;
    }

    @Override
    public String toString() {
        return "(assert " + assertion.toString() + ")";
    }
}
