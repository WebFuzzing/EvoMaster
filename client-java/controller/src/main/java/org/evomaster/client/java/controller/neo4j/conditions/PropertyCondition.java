package org.evomaster.client.java.controller.neo4j.conditions;

import java.util.Objects;

/**
 * Represents a property equality condition {@code n.key = value} extracted from an inline property
 * map like {@code {name: "Alice"}}. The value is a typed {@link Operand}, the same representation a
 * WHERE comparison uses, so {@code {age: 25 + 5}} folds to a literal and {@code {at: time("11:11")}}
 * keeps the expression as a {@link RawOperand} instead of guessing a value.
 */
public class PropertyCondition implements CypherCondition {

    private final String variableName;
    private final String propertyKey;
    private final Operand value;

    public PropertyCondition(String variableName, String propertyKey, Operand value) {
        this.variableName = Objects.requireNonNull(variableName, "variableName must not be null");
        this.propertyKey = Objects.requireNonNull(propertyKey, "propertyKey must not be null");
        this.value = Objects.requireNonNull(value, "value must not be null");
    }

    public String getVariableName() {
        return variableName;
    }

    public String getPropertyKey() {
        return propertyKey;
    }

    public Operand getValue() {
        return value;
    }

    @Override
    public String toString() {
        return variableName + "." + propertyKey + " = " + value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PropertyCondition that = (PropertyCondition) o;
        if (!Objects.equals(variableName, that.variableName)) return false;
        if (!Objects.equals(propertyKey, that.propertyKey)) return false;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        int result = variableName != null ? variableName.hashCode() : 0;
        result = 31 * result + (propertyKey != null ? propertyKey.hashCode() : 0);
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
    }
}
