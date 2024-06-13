package org.evomaster.solver.smtlib;

public class DeclareConstSMTNode extends SMTNode {
    final String name;
    final String type;

    public DeclareConstSMTNode(String name, String type) {
        this.name = name;
        this.type = type;
    }

    @Override
    public String toString() {
        return "(declare-const " + name + " " + type + ")";
    }
}
