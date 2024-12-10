package org.evomaster.solver.smtlib;

import org.evomaster.solver.smtlib.assertion.Assertion;

public class AssertSMTNode extends SMTNode {
    private final Assertion assertion;

    public AssertSMTNode(Assertion assertion) {
        this.assertion = assertion;
    }

    public Assertion getAssertion() {
        return assertion;
    }

    @Override
    public String toString() {
        return "(assert " + assertion.toString() + ")";
    }
}
