package org.evomaster.client.java.controller.neo4j.heuristics;

import org.evomaster.client.java.controller.neo4j.data.Neo4jEdge;
import org.evomaster.client.java.controller.neo4j.data.Neo4jNode;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A single structural mapping {@code m = (μ, ε)} from a query pattern {@code P_s} onto a graph
 * {@code G}: it binds each pattern node variable to a graph node ({@code μ}) and each pattern edge
 * variable to a graph relationship ({@code ε}). Bindings are keyed by the variable name the parser
 * assigned (a real name like {@code n}, or an anonymous {@code _anon_node_0} / {@code _anon_rel_0}),
 * which is shared between the {@code PatternEdge}/{@code PatternNode} and the conditions that refer
 * to it, so condition valuation can resolve a variable to its bound graph element.
 */
class Neo4jMapping {

    private final Map<String, Neo4jNode> nodeBindings;
    private final Map<String, Neo4jEdge> edgeBindings;

    Neo4jMapping() {
        this.nodeBindings = new LinkedHashMap<>();
        this.edgeBindings = new LinkedHashMap<>();
    }

    private Neo4jMapping(Map<String, Neo4jNode> nodeBindings, Map<String, Neo4jEdge> edgeBindings) {
        this.nodeBindings = new LinkedHashMap<>(Objects.requireNonNull(nodeBindings, "nodeBindings must not be null"));
        this.edgeBindings = new LinkedHashMap<>(Objects.requireNonNull(edgeBindings, "edgeBindings must not be null"));
    }

    Neo4jMapping copy() {
        return new Neo4jMapping(nodeBindings, edgeBindings);
    }

    /**
     * The graph node bound to the given pattern variable, or {@code null} if this mapping does not
     * bind one. A {@code null} return is a normal outcome, not an error: it is how
     * {@code resolveProperty} tells a node variable from a relationship variable, and how a condition
     * on an unbound variable is detected (it yields no {@code ρ}, and the aggregation skips it).
     * <p>
     * The variable itself is never {@code null} — the parser names every pattern element, and every
     * condition, operand and edge endpoint guards its variable name at construction. Guarding it here
     * keeps a {@code null} name, which would be a bug, from silently looking like an unbound variable.
     */
    Neo4jNode getNode(String variable) {
        Objects.requireNonNull(variable, "variable must not be null");
        return nodeBindings.get(variable);
    }

    /**
     * The graph relationship bound to the given pattern variable, or {@code null} when this mapping
     * does not bind it. Same contract as {@link #getNode(String)}.
     */
    Neo4jEdge getEdge(String variable) {
        Objects.requireNonNull(variable, "variable must not be null");
        return edgeBindings.get(variable);
    }

    boolean isNodeBound(String variable) {
        Objects.requireNonNull(variable, "variable must not be null");
        return nodeBindings.containsKey(variable);
    }

    void bindNode(String variable, Neo4jNode node) {
        Objects.requireNonNull(variable, "variable must not be null");
        Objects.requireNonNull(node, "node must not be null");
        nodeBindings.put(variable, node);
    }

    void bindEdge(String variable, Neo4jEdge edge) {
        Objects.requireNonNull(variable, "variable must not be null");
        Objects.requireNonNull(edge, "edge must not be null");
        edgeBindings.put(variable, edge);
    }

    /**
     * True when this graph edge is already used by some pattern edge in this mapping. Cypher
     * enforces relationship uniqueness within a single MATCH, so the enumerator avoids reusing one.
     */
    boolean usesEdge(Neo4jEdge edge) {
        Objects.requireNonNull(edge, "edge must not be null");
        return edgeBindings.containsValue(edge);
    }

    @Override
    public String toString() {
        return "Neo4jMapping{nodes=" + nodeBindings + ", edges=" + edgeBindings + "}";
    }
}
