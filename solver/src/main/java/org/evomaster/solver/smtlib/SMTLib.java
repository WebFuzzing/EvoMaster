package org.evomaster.solver.smtlib;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * SMTLib class Abstraction to generate SMT-LIB files
 */
public class SMTLib {

    private final List<SMTNode> nodes = new ArrayList<>();

    public void addNode(SMTNode node) {
        nodes.add(node);
    }

    // Two SMTLib are equal if they expose the same smt file
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SMTLib smtLib = (SMTLib) o;
        return Objects.equals(this.toString(), smtLib.toString());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(nodes);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (SMTNode node : nodes) {
            sb.append(node.toString()).append("\n");
        }
        return sb.toString();
    }
}
