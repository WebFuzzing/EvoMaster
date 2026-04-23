package org.evomaster.solver.smtlib;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The SMTLib class provides an abstraction for generating SMT-LIB files.
 * It manages a collection of SMT nodes, which represent different components
 * of the SMT-LIB format used for constraint solving.
 */
public class SMTLib {

    // List to hold SMT nodes, which are the building blocks of SMT-LIB files
    private final List<SMTNode> nodes = new ArrayList<>();

    /**
     * Adds a single SMT node to the list of nodes.
     *
     * @param node the SMTNode to be added
     */
    public void addNode(SMTNode node) {
        nodes.add(node);
    }

    /**
     * Adds a list of SMT nodes to the existing list.
     *
     * @param newNodes the list of SMTNodes to be added
     */
    public void addNodes(List<SMTNode> newNodes) {
        if (newNodes != null && !newNodes.isEmpty()) {
            nodes.addAll(newNodes);
        }
    }

    /**
     * Checks equality of this SMTLib instance with another object.
     * Two SMTLib instances are considered equal if they represent the same SMT-LIB content.
     *
     * @param o the object to be compared with this SMTLib instance
     * @return true if the objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SMTLib smtLib = (SMTLib) o;
        return Objects.equals(this.toString(), smtLib.toString());
    }

    /**
     * Computes the hash code for this SMTLib instance.
     * The hash code is based on the string representation of the SMT-LIB content.
     *
     * @return the hash code value for this SMTLib instance
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(nodes.toString());
    }

    /**
     * Returns a string representation of the SMT-LIB content.
     * Each SMTNode is converted to its string representation and appended to the result.
     *
     * @return the SMT-LIB content as a string
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (SMTNode node : nodes) {
            sb.append(node.toString()).append("\n");
        }
        return sb.toString();
    }
}



