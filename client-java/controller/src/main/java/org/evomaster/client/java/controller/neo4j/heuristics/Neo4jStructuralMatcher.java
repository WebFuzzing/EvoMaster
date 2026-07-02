package org.evomaster.client.java.controller.neo4j.heuristics;

import org.evomaster.client.java.controller.neo4j.data.Neo4jGraph;
import org.evomaster.client.java.controller.neo4j.data.Neo4jNode;
import org.evomaster.client.java.controller.neo4j.data.Neo4jRelationship;
import org.evomaster.client.java.controller.neo4j.operations.MatchPattern;
import org.evomaster.client.java.controller.neo4j.operations.PatternEdge;
import org.evomaster.client.java.controller.neo4j.operations.PatternNode;
import org.evomaster.client.java.utils.SimpleLogger;

import java.util.ArrayList;
import java.util.List;

/**
 * Enumerates {@code matched_elements(P_s, G)}: every structurally valid mapping {@code (μ, ε)} of a
 * stripped pattern onto a graph. A mapping binds each pattern node variable to a graph node and each
 * pattern edge variable to a graph relationship such that the relationship's endpoints agree with the
 * node bindings (an undirected edge matches either orientation), repeated variables resolve to the
 * same element (equijoins), and no graph relationship is used twice in one mapping.
 */
class Neo4jStructuralMatcher {

    /**
     * Upper bound on the number of mappings enumerated, to avoid combinatorial blow-up on large
     * graphs / patterns. Once reached, enumeration stops and the partial set is used.
     */
    static final int MAX_NUM_MAPPINGS = 2000;

    List<Neo4jMapping> matchedElements(MatchPattern pattern, Neo4jGraph graph) {
        List<Neo4jMapping> results = new ArrayList<>();
        List<PatternEdge> edges = pattern.getEdges();

        if (edges.isEmpty()) {
            extendIsolatedNodes(pattern.getNodes(), 0, new Neo4jMapping(), graph, results);
        } else {
            backtrackEdges(edges, 0, new Neo4jMapping(), pattern, graph, results);
        }

        if (results.size() >= MAX_NUM_MAPPINGS) {
            SimpleLogger.uniqueWarn("Neo4j structural matching hit the cap of " + MAX_NUM_MAPPINGS
                    + " mappings; the heuristic is computed over a partial set of mappings.");
        }
        return results;
    }

    private void backtrackEdges(List<PatternEdge> edges, int index, Neo4jMapping current,
                                MatchPattern pattern, Neo4jGraph graph, List<Neo4jMapping> results) {
        if (results.size() >= MAX_NUM_MAPPINGS) {
            return;
        }
        if (index == edges.size()) {
            extendIsolatedNodes(pattern.getNodes(), 0, current, graph, results);
            return;
        }

        PatternEdge edge = edges.get(index);
        for (Neo4jRelationship rel : graph.getRelationships()) {
            if (current.usesRelationship(rel)) {
                continue;
            }
            tryOrientation(current, edge, rel, rel.getSourceId(), rel.getTargetId(),
                    edges, index, pattern, graph, results);
            if (!edge.isDirected()) {
                tryOrientation(current, edge, rel, rel.getTargetId(), rel.getSourceId(),
                        edges, index, pattern, graph, results);
            }
            if (results.size() >= MAX_NUM_MAPPINGS) {
                return;
            }
        }
    }

    /**
     * Binds {@code edge} to {@code rel} with the given endpoint orientation, if consistent with the
     * bindings already in {@code current}, then recurses to the next edge.
     */
    private void tryOrientation(Neo4jMapping current, PatternEdge edge, Neo4jRelationship rel,
                                String sourceNodeId, String targetNodeId,
                                List<PatternEdge> edges, int index, MatchPattern pattern,
                                Neo4jGraph graph, List<Neo4jMapping> results) {
        if (!consistentNode(current, edge.getSourceVariable(), sourceNodeId)
                || !consistentNode(current, edge.getTargetVariable(), targetNodeId)) {
            return;
        }
        Neo4jMapping next = current.copy();
        next.bindNode(edge.getSourceVariable(), graph.getNodeById(sourceNodeId));
        next.bindNode(edge.getTargetVariable(), graph.getNodeById(targetNodeId));
        next.bindEdge(edge.getVariableName(), rel);
        backtrackEdges(edges, index + 1, next, pattern, graph, results);
    }

    private boolean consistentNode(Neo4jMapping current, String variable, String nodeId) {
        if (!current.isNodeBound(variable)) {
            return true;
        }
        Neo4jNode bound = current.getNode(variable);
        return bound != null && bound.getId().equals(nodeId);
    }

    /**
     * Cartesian extension over pattern nodes not yet bound by an edge: each ranges over all graph
     * nodes. For a fully edge-connected pattern this is a no-op (all nodes already bound).
     */
    private void extendIsolatedNodes(List<PatternNode> nodes, int index, Neo4jMapping current,
                                     Neo4jGraph graph, List<Neo4jMapping> results) {
        if (results.size() >= MAX_NUM_MAPPINGS) {
            return;
        }
        if (index == nodes.size()) {
            results.add(current.copy());
            return;
        }
        PatternNode node = nodes.get(index);
        if (current.isNodeBound(node.getVariableName())) {
            extendIsolatedNodes(nodes, index + 1, current, graph, results);
            return;
        }
        if (graph.getNodes().isEmpty()) {
            return;
        }
        for (Neo4jNode graphNode : graph.getNodes()) {
            Neo4jMapping next = current.copy();
            next.bindNode(node.getVariableName(), graphNode);
            extendIsolatedNodes(nodes, index + 1, next, graph, results);
            if (results.size() >= MAX_NUM_MAPPINGS) {
                return;
            }
        }
    }
}
