package org.evomaster.client.java.controller.neo4j.operations;

import org.evomaster.client.java.controller.neo4j.conditions.CypherCondition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A quantified path pattern (QPP), i.e. a sub-pattern repeated a number of times,
 * such as {@code ((a)-[:KNOWS]->(b)){1,3}}.
 * <br>
 * Unlike a variable-length relationship ({@code *1..3}), which repeats a single
 * edge, a QPP repeats an entire {@link MatchPattern} (multiple nodes and edges,
 * and possibly nested QPPs). The repetition bounds are {@code [min, max]}, where a
 * {@code null} max means unbounded ({@code +} or {@code *}).
 * <br>
 * The labels, relationship types, properties and inline {@code WHERE} that constrain the
 * sub-pattern are kept in {@link #getConditions()}, scoped to this QPP rather than mixed into
 * the outer operation's list — so when the calculator expands the repetition it can clone the
 * sub-pattern's conditions per hop.
 * <br>
 * {@link #getEntryVariable()} and {@link #getExitVariable()} are the outer pattern variables this
 * QPP splices between (e.g. {@code a} and {@code b} in {@code (a) ((x)-[]->(y)){1,3} (b)}), so the
 * calculator can bind the first/last repetition's boundary node to the surrounding pattern. Either
 * is {@code null} when there is no outer node on that side (the QPP opens or closes the pattern).
 * When two QPPs are directly adjacent with no node between them, the parser binds them to the same
 * synthesized variable, so this is never {@code null} because two QPPs happen to touch.
 */
public class QuantifiedPathPattern {

    private final MatchPattern subPattern;
    private final List<CypherCondition> conditions;
    private final int min;
    private final Integer max;
    private final String entryVariable;
    private final String exitVariable;

    public QuantifiedPathPattern(MatchPattern subPattern, int min, Integer max) {
        this(subPattern, Collections.<CypherCondition>emptyList(), min, max, null, null);
    }

    public QuantifiedPathPattern(MatchPattern subPattern, List<CypherCondition> conditions, int min, Integer max) {
        this(subPattern, conditions, min, max, null, null);
    }

    public QuantifiedPathPattern(MatchPattern subPattern, List<CypherCondition> conditions, int min, Integer max,
                                 String entryVariable, String exitVariable) {
        this.subPattern = Objects.requireNonNull(subPattern, "subPattern must not be null");
        this.conditions = Collections.unmodifiableList(
                conditions != null ? new ArrayList<>(conditions) : new ArrayList<>());
        this.min = min;
        this.max = max;
        this.entryVariable = entryVariable;
        this.exitVariable = exitVariable;
    }

    public MatchPattern getSubPattern() {
        return subPattern;
    }

    /**
     * Returns the conditions scoped to this sub-pattern (labels, relationship types, properties,
     * inline WHERE), in source order. Empty when the sub-pattern is purely structural.
     */
    public List<CypherCondition> getConditions() {
        return conditions;
    }

    public int getMin() {
        return min;
    }

    public Integer getMax() {
        return max;
    }

    public boolean isUnboundedMax() {
        return max == null;
    }

    /** The outer variable immediately before this QPP, or {@code null} if it opens the pattern. */
    public String getEntryVariable() {
        return entryVariable;
    }

    /** The outer variable immediately after this QPP, or {@code null} if it closes the pattern. */
    public String getExitVariable() {
        return exitVariable;
    }

    /** Returns a copy of this QPP with its entry/exit variables replaced. */
    public QuantifiedPathPattern withBoundary(String newEntryVariable, String newExitVariable) {
        return new QuantifiedPathPattern(subPattern, conditions, min, max, newEntryVariable, newExitVariable);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("QuantifiedPathPattern{");
        if (entryVariable != null) {
            sb.append("entry=").append(entryVariable).append(", ");
        }
        sb.append(subPattern);
        if (!conditions.isEmpty()) {
            sb.append(", conditions=").append(conditions);
        }
        sb.append(" {").append(min).append(",").append(max != null ? max : "").append("}");
        if (exitVariable != null) {
            sb.append(", exit=").append(exitVariable);
        }
        sb.append("}");
        return sb.toString();
    }
}
