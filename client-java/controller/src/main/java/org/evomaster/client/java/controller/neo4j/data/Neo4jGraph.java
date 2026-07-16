package org.evomaster.client.java.controller.neo4j.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * An in-memory snapshot of a Neo4j graph ({@code G}): the set of nodes and relationships against
 * which a parsed query is scored. Built either by hand in tests or by reading the live database
 * through the driver. Nodes are indexed by id so a relationship's endpoints can be resolved quickly.
 */
public class Neo4jGraph {

    private final List<Neo4jNode> nodes;
    private final List<Neo4jEdge> edges;
    private final Map<String, Neo4jNode> nodesById;

    public Neo4jGraph(List<Neo4jNode> nodes, List<Neo4jEdge> edges) {
        this.nodes = nodes != null ? new ArrayList<>(nodes) : new ArrayList<>();
        this.edges = edges != null ? new ArrayList<>(edges) : new ArrayList<>();
        this.nodesById = new LinkedHashMap<>();
        for (Neo4jNode n : this.nodes) {
            nodesById.put(n.getId(), n);
        }
    }

    public List<Neo4jNode> getNodes() {
        return Collections.unmodifiableList(nodes);
    }

    public List<Neo4jEdge> getEdges() {
        return Collections.unmodifiableList(edges);
    }

    public int nodeCount() {
        return nodes.size();
    }

    public Neo4jNode getNodeById(String id) {
        return nodesById.get(id);
    }

    @Override
    public String toString() {
        return "Neo4jGraph{nodes=" + nodes.size() + ", edges=" + edges.size() + "}";
    }
}
