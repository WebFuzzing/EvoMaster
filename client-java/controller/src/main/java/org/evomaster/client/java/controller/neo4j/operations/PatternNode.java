package org.evomaster.client.java.controller.neo4j.operations;

/**
 * Represents a node in a structural MATCH pattern (P_s).
 * Contains only the variable name, not labels or properties (those are conditions).
 */
public class PatternNode {

    private final String variableName;

    public PatternNode(String variableName) {
        this.variableName = variableName;
    }

    public String getVariableName() {
        return variableName;
    }

    @Override
    public String toString() {
        return "(" + (variableName != null ? variableName : "") + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PatternNode that = (PatternNode) o;
        if (variableName == null) return that.variableName == null;
        return variableName.equals(that.variableName);
    }

    @Override
    public int hashCode() {
        return variableName != null ? variableName.hashCode() : 0;
    }
}
