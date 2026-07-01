package org.evomaster.client.java.controller.neo4j.conditions;

import java.util.Objects;

/**
 * Represents a label condition: n:Label
 * Extracted from patterns like (n:Person) or (n:Person:Employee)
 */
public class LabelCondition implements CypherCondition {

    private final String variableName;
    private final String label;

    public LabelCondition(String variableName, String label) {
        this.variableName = Objects.requireNonNull(variableName, "variableName must not be null");
        this.label = Objects.requireNonNull(label, "label must not be null");
    }

    public String getVariableName() {
        return variableName;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return variableName + ":" + label;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LabelCondition that = (LabelCondition) o;
        if (!Objects.equals(variableName, that.variableName)) return false;
        return Objects.equals(label, that.label);
    }

    @Override
    public int hashCode() {
        int result = variableName != null ? variableName.hashCode() : 0;
        result = 31 * result + (label != null ? label.hashCode() : 0);
        return result;
    }
}
