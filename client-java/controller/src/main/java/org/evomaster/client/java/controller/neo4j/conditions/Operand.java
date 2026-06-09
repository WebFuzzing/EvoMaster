package org.evomaster.client.java.controller.neo4j.conditions;

/**
 * One side of a comparison in a WHERE clause: a {@link PropertyOperand} resolved from the
 * matched element, a {@link LiteralOperand} constant, or a {@link RawOperand} kept unchanged.
 * Modelling each side explicitly lets {@code n.age > m.age} be told apart from {@code n.age > "m.age"}.
 */
public interface Operand {
}
