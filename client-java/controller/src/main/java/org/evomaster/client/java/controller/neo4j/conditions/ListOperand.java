package org.evomaster.client.java.controller.neo4j.conditions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A list operand, e.g. the right-hand side of x IN [1, 2, 3]. Holds the element operands
 * so a membership test can be valuated element by element.
 */
public final class ListOperand implements Operand {

    private final List<Operand> elements;

    public ListOperand(List<Operand> elements) {
        this.elements = Collections.unmodifiableList(
                elements != null ? new ArrayList<>(elements) : new ArrayList<>());
    }

    public List<Operand> getElements() {
        return elements;
    }

    @Override
    public String toString() {
        return elements.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return elements.equals(((ListOperand) o).elements);
    }

    @Override
    public int hashCode() {
        return elements.hashCode();
    }
}
