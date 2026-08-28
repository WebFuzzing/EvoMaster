package org.evomaster.client.java.controller.neo4j.conditions;

/**
 * Visitor over the typed {@link CypherCondition} boolean tree the parser produces. Each concrete
 * condition dispatches to its own {@code visit} method through {@link CypherCondition#accept}, so a
 * consumer handles every case explicitly and the compiler flags any implementation that forgets one
 * (no {@code instanceof} chain, no silent fall-through for an unhandled condition type).
 *
 * @param <T> the result type each visit produces (e.g. a {@code Truthness} for the heuristics).
 */
public interface CypherConditionVisitor<T> {

    T visitLabel(LabelCondition condition);

    T visitAnyLabel(AnyLabelCondition condition);

    T visitType(TypeCondition condition);

    T visitProperty(PropertyCondition condition);

    T visitComparison(ComparisonCondition condition);

    T visitAnd(AndCondition condition);

    T visitOr(OrCondition condition);

    T visitXor(XorCondition condition);

    T visitNot(NotCondition condition);

    T visitRaw(RawCondition condition);
}
