package org.evomaster.client.java.controller.neo4j.conditions;

import java.util.Objects;

/**
 * The any-label wildcard % in a label expression, e.g. (n:%). Satisfied when the element
 * bound to the variable has a non-empty label set.
 */
public class AnyLabelCondition implements CypherCondition {

    private final String variableName;

    public AnyLabelCondition(String variableName) {
        this.variableName = variableName;
    }

    public String getVariableName() {
        return variableName;
    }

    @Override
    public String toString() {
        return variableName + ":%";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return Objects.equals(variableName, ((AnyLabelCondition) o).variableName);
    }

    @Override
    public int hashCode() {
        return variableName != null ? variableName.hashCode() : 0;
    }
}
