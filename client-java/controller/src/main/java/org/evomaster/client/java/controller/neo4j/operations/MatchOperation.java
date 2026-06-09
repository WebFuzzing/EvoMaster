package org.evomaster.client.java.controller.neo4j.operations;

import org.evomaster.client.java.controller.neo4j.conditions.CypherCondition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a parsed MATCH query containing the structural pattern and extracted conditions.
 * <p>
 * The structural pattern contains only the graph topology (nodes and edges).
 * Conditions include labels, relationship types, property constraints, and WHERE clause predicates.
 * <p>
 * A query may assign several path variables ({@code MATCH p = ..., q = ...}); all are kept in order.
 * {@link #isOptional()} is true when the query involves an {@code OPTIONAL MATCH} (left-join semantics).
 */
public class MatchOperation extends CypherQueryOperation {

    private final MatchPattern pattern;
    private final List<CypherCondition> conditions;
    private final List<String> pathVariables;
    private final boolean optional;

    public MatchOperation(MatchPattern pattern, List<CypherCondition> conditions) {
        this(pattern, conditions, (String) null);
    }

    public MatchOperation(MatchPattern pattern, List<CypherCondition> conditions, String pathVariable) {
        this(pattern, conditions,
                pathVariable != null ? Collections.singletonList(pathVariable) : Collections.<String>emptyList(),
                false);
    }

    public MatchOperation(MatchPattern pattern, List<CypherCondition> conditions,
                          List<String> pathVariables, boolean optional) {
        this.pattern = pattern;
        this.conditions = conditions != null ? new ArrayList<>(conditions) : new ArrayList<>();
        this.pathVariables = pathVariables != null ? new ArrayList<>(pathVariables) : new ArrayList<>();
        this.optional = optional;
    }

    /**
     * Returns the structural pattern containing nodes and edges.
     */
    public MatchPattern getPattern() {
        return pattern;
    }

    /**
     * Returns all conditions extracted from both inline patterns and WHERE clause.
     */
    public List<CypherCondition> getConditions() {
        return Collections.unmodifiableList(conditions);
    }

    /**
     * Returns the first path variable assigned by the pattern (e.g. {@code path = ...}), or null if
     * none. Kept for callers that expect a single assignment; {@link #getPathVariables()} returns all.
     */
    public String getPathVariable() {
        return pathVariables.isEmpty() ? null : pathVariables.get(0);
    }

    /**
     * Returns every path variable assigned by the query, in source order (empty if there are none).
     */
    public List<String> getPathVariables() {
        return Collections.unmodifiableList(pathVariables);
    }

    /**
     * Returns true when the query involves an {@code OPTIONAL MATCH}. The match still succeeds with
     * no binding when the pattern is absent, so a consumer should not penalize a missing optional part.
     */
    public boolean isOptional() {
        return optional;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("MatchOperation{");
        if (optional) {
            sb.append("optional, ");
        }
        if (!pathVariables.isEmpty()) {
            sb.append("pathVariables=").append(pathVariables).append(", ");
        }
        sb.append("pattern=").append(pattern);
        sb.append(", conditions=[");
        for (int i = 0; i < conditions.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(conditions.get(i));
        }
        sb.append("]}");
        return sb.toString();
    }
}
