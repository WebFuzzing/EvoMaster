package org.evomaster.client.java.controller.neo4j.conditions;

/**
 * The arithmetic connectives an {@link ArithmeticOperand} can carry. Binary operators map to the
 * Cypher tokens {@code + - * / % ^}; {@link #NEGATE} is unary minus (its operand is the left side).
 */
public enum ArithmeticOperator {
    PLUS,
    MINUS,
    TIMES,
    DIVIDE,
    MODULO,
    POWER,
    NEGATE
}
