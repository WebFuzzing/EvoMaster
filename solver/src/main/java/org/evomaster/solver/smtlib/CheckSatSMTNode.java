package org.evomaster.solver.smtlib;

public class CheckSatSMTNode extends SMTNode {
    @Override
    public String toString() {
        return "(check-sat)";
    }
}
