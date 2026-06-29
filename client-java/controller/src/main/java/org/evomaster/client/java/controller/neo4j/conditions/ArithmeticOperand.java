package org.evomaster.client.java.controller.neo4j.conditions;

import java.util.Objects;

/**
 * An operand that is an arithmetic expression over other operands, e.g. {@code p.age + 5} or
 * {@code -n.weight}. Keeps the structure (operator + sub-operands) instead of the original text,
 * so the inner {@link PropertyOperand}s stay resolvable per mapping and the heuristics calculator
 * can valuate {@code v(x op y) = apply(op, v(left), v(right))}.
 *
 * <p>Fully-literal subtrees are folded to a {@link LiteralOperand} at parse time, so this class only
 * ever holds an expression that still depends on the graph (at least one property reference).
 *
 * <p>For {@link ArithmeticOperator#NEGATE} (unary minus) the operand is {@link #getLeft()} and
 * {@link #getRight()} is {@code null}.
 */
public final class ArithmeticOperand implements Operand {

    private final ArithmeticOperator operator;
    private final Operand left;
    private final Operand right;

    public ArithmeticOperand(ArithmeticOperator operator, Operand left, Operand right) {
        this.operator = Objects.requireNonNull(operator, "operator must not be null");
        this.left = Objects.requireNonNull(left, "left operand must not be null");
        this.right = right;
    }

    public ArithmeticOperator getOperator() {
        return operator;
    }

    public Operand getLeft() {
        return left;
    }

    public Operand getRight() {
        return right;
    }

    @Override
    public String toString() {
        if (operator == ArithmeticOperator.NEGATE) {
            return "-" + left;
        }
        return "(" + left + " " + symbol() + " " + right + ")";
    }

    private String symbol() {
        switch (operator) {
            case PLUS: return "+";
            case MINUS: return "-";
            case TIMES: return "*";
            case DIVIDE: return "/";
            case MODULO: return "%";
            case POWER: return "^";
            default: return "?";
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArithmeticOperand that = (ArithmeticOperand) o;
        return operator == that.operator
                && Objects.equals(left, that.left)
                && Objects.equals(right, that.right);
    }

    @Override
    public int hashCode() {
        int result = operator != null ? operator.hashCode() : 0;
        result = 31 * result + (left != null ? left.hashCode() : 0);
        result = 31 * result + (right != null ? right.hashCode() : 0);
        return result;
    }
}
