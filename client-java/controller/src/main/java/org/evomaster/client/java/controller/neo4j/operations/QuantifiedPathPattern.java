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
 */
public class QuantifiedPathPattern {

    private final MatchPattern subPattern;
    private final List<CypherCondition> conditions;
    private final int min;
    private final Integer max;

    public QuantifiedPathPattern(MatchPattern subPattern, int min, Integer max) {
        this(subPattern, Collections.<CypherCondition>emptyList(), min, max);
    }

    public QuantifiedPathPattern(MatchPattern subPattern, List<CypherCondition> conditions, int min, Integer max) {
        this.subPattern = Objects.requireNonNull(subPattern, "subPattern must not be null");
        this.conditions = Collections.unmodifiableList(
                conditions != null ? new ArrayList<>(conditions) : new ArrayList<>());
        this.min = min;
        this.max = max;
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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("QuantifiedPathPattern{").append(subPattern);
        if (!conditions.isEmpty()) {
            sb.append(", conditions=").append(conditions);
        }
        sb.append(" {").append(min).append(",").append(max != null ? max : "").append("}}");
        return sb.toString();
    }
}
