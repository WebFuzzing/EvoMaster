package org.evomaster.client.java.controller.neo4j.conditions;

import java.util.Objects;

/**
 * A query parameter reference such as {@code $title}, whose value is not written in the query but
 * supplied at execution time. Carries the parameter name without the leading {@code $}, so the value
 * can later be looked up in the parameter map captured alongside the query.
 * <p>
 * Told apart from {@link RawOperand} on purpose. Both are unresolved when the query is parsed, but a
 * parameter has a value that can be recovered, whereas a {@code RawOperand} is an expression the model
 * chose not to decompose at all.
 */
public final class ParameterOperand implements Operand {

    private final String name;

    public ParameterOperand(String name) {
        this.name = Objects.requireNonNull(name, "name must not be null");
    }

    /** The parameter name without the leading {@code $}: {@code title} for {@code $title}. */
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "$" + name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return Objects.equals(name, ((ParameterOperand) o).name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
