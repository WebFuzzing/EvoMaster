package org.evomaster.solver.smtlib;

public class DeclareConst extends SMTNode {
    final String name;
    final String type;

    public DeclareConst(String name, String type) {
        this.name = name;
        this.type = type;
    }

    @Override
    public String toString() {
        return "(declare-const " + name + " " + type + ")";
    }
}
