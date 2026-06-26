package org.evomaster.client.java.controller.neo4j.operations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents the structural MATCH pattern (P_s) after stripping labels and properties.
 * Contains nodes, edges, and any quantified path patterns (repeated sub-patterns).
 */
public class MatchPattern {

    private final List<PatternNode> nodes;
    private final List<PatternEdge> edges;
    private final List<QuantifiedPathPattern> quantifiedPaths;

    public MatchPattern(List<PatternNode> nodes, List<PatternEdge> edges) {
        this(nodes, edges, null);
    }

    public MatchPattern(List<PatternNode> nodes, List<PatternEdge> edges,
                        List<QuantifiedPathPattern> quantifiedPaths) {
        this.nodes = Collections.unmodifiableList(
                nodes != null ? new ArrayList<>(nodes) : new ArrayList<>());
        this.edges = Collections.unmodifiableList(
                edges != null ? new ArrayList<>(edges) : new ArrayList<>());
        this.quantifiedPaths = Collections.unmodifiableList(
                quantifiedPaths != null ? new ArrayList<>(quantifiedPaths) : new ArrayList<>());
    }

    public List<PatternNode> getNodes() {
        return nodes;
    }

    public List<PatternEdge> getEdges() {
        return edges;
    }

    public List<QuantifiedPathPattern> getQuantifiedPaths() {
        return quantifiedPaths;
    }

    public int nodeCount() {
        return nodes.size();
    }

    public int edgeCount() {
        return edges.size();
    }

    public int quantifiedPathCount() {
        return quantifiedPaths.size();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("MatchPattern{nodes=[");
        for (int i = 0; i < nodes.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(nodes.get(i));
        }
        sb.append("], edges=[");
        for (int i = 0; i < edges.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(edges.get(i));
        }
        sb.append("]");
        if (!quantifiedPaths.isEmpty()) {
            sb.append(", quantifiedPaths=[");
            for (int i = 0; i < quantifiedPaths.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(quantifiedPaths.get(i));
            }
            sb.append("]");
        }
        sb.append("}");
        return sb.toString();
    }
}
