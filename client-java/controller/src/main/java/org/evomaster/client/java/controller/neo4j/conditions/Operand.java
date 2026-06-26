package org.evomaster.client.java.controller.neo4j.conditions;

/**
 * One operand of a condition: a side of a WHERE comparison or an inline property value. Concrete
 * kinds are {@link PropertyOperand} (a {@code variable.key} resolved from the matched element),
 * {@link LiteralOperand} (a constant), {@link ArithmeticOperand} (an arithmetic expression),
 * {@link ListOperand} (an {@code IN} list), and {@link RawOperand} (kept verbatim when not
 * decomposed). Modelling each explicitly lets {@code n.age > m.age} be told apart from
 * {@code n.age > "m.age"}.
 */
public interface Operand {
}
