package org.evomaster.solver.smtlib;

public class CheckSat extends SMTNode {
    @Override
    public String toString() {
        return "(check-sat)";
    }
}
