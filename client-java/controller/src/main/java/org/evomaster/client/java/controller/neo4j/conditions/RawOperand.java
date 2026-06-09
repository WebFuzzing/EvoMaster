package org.evomaster.client.java.controller.neo4j.conditions;

import java.util.Objects;

/**
 * An operand kept unchanged because the model does not decompose it: a function call, a parameter,
 * or string concatenation. Carries the original text so nothing is dropped.
 */
public final class RawOperand implements Operand {

    private final String text;

    public RawOperand(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    @Override
    public String toString() {
        return text;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return Objects.equals(text, ((RawOperand) o).text);
    }

    @Override
    public int hashCode() {
        return text != null ? text.hashCode() : 0;
    }
}
