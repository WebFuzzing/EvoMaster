package org.evomaster.client.java.controller.neo4j.conditions;

import java.util.Objects;

/**
 * Represents a relationship type condition: type(r) = T
 * Extracted from patterns like -[:KNOWS]-> or -[r:WORKS_AT]->
 */
public class TypeCondition implements CypherCondition {

    private final String variableName;
    private final String type;

    public TypeCondition(String variableName, String type) {
        this.variableName = Objects.requireNonNull(variableName, "variableName must not be null");
        this.type = Objects.requireNonNull(type, "type must not be null");
    }

    public String getVariableName() {
        return variableName;
    }

    public String getType() {
        return type;
    }

    @Override
    public String toString() {
        return "type(" + variableName + ") = " + type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TypeCondition that = (TypeCondition) o;
        if (!Objects.equals(variableName, that.variableName)) return false;
        return Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        int result = variableName != null ? variableName.hashCode() : 0;
        result = 31 * result + (type != null ? type.hashCode() : 0);
        return result;
    }
}
