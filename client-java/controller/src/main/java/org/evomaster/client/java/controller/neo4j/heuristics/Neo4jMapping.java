package org.evomaster.client.java.controller.neo4j.heuristics;

import org.evomaster.client.java.controller.neo4j.data.Neo4jNode;
import org.evomaster.client.java.controller.neo4j.data.Neo4jRelationship;

import java.util.LinkedHashMap;
import java.util.Map;

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
    private final Map<String, Neo4jRelationship> edgeBindings;

    Neo4jMapping() {
        this.nodeBindings = new LinkedHashMap<>();
        this.edgeBindings = new LinkedHashMap<>();
    }

    private Neo4jMapping(Map<String, Neo4jNode> nodeBindings, Map<String, Neo4jRelationship> edgeBindings) {
        this.nodeBindings = new LinkedHashMap<>(nodeBindings);
        this.edgeBindings = new LinkedHashMap<>(edgeBindings);
    }

    Neo4jMapping copy() {
        return new Neo4jMapping(nodeBindings, edgeBindings);
    }

    Neo4jNode getNode(String variable) {
        return nodeBindings.get(variable);
    }

    Neo4jRelationship getEdge(String variable) {
        return edgeBindings.get(variable);
    }

    boolean isNodeBound(String variable) {
        return nodeBindings.containsKey(variable);
    }

    void bindNode(String variable, Neo4jNode node) {
        nodeBindings.put(variable, node);
    }

    void bindEdge(String variable, Neo4jRelationship relationship) {
        edgeBindings.put(variable, relationship);
    }

    /**
     * True when this graph relationship is already used by some pattern edge in this mapping. Cypher
     * enforces relationship uniqueness within a single MATCH, so the enumerator avoids reusing one.
     */
    boolean usesRelationship(Neo4jRelationship relationship) {
        return edgeBindings.containsValue(relationship);
    }

    @Override
    public String toString() {
        return "Neo4jMapping{nodes=" + nodeBindings + ", edges=" + edgeBindings + "}";
    }
}
