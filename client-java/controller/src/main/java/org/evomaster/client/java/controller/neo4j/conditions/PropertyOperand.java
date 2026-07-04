package org.evomaster.client.java.controller.neo4j.conditions;

import java.util.Objects;

/**
 * A property reference operand: variable.key (e.g. n.age), resolved from the graph
 * element bound to the variable under the current mapping.
 */
public final class PropertyOperand implements Operand {

    private final String variableName;
    private final String propertyKey;

    public PropertyOperand(String variableName, String propertyKey) {
        this.variableName = Objects.requireNonNull(variableName, "variableName must not be null");
        this.propertyKey = Objects.requireNonNull(propertyKey, "propertyKey must not be null");
    }

    public String getVariableName() {
        return variableName;
    }

    public String getPropertyKey() {
        return propertyKey;
    }

    @Override
    public String toString() {
        return variableName + "." + propertyKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PropertyOperand that = (PropertyOperand) o;
        return Objects.equals(variableName, that.variableName)
                && Objects.equals(propertyKey, that.propertyKey);
    }

    @Override
    public int hashCode() {
        int result = variableName != null ? variableName.hashCode() : 0;
        result = 31 * result + (propertyKey != null ? propertyKey.hashCode() : 0);
        return result;
    }
}
