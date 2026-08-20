package org.evomaster.client.java.controller.internal.db.neo4j;

import org.evomaster.client.java.controller.neo4j.data.Neo4jEdge;
import org.evomaster.client.java.controller.neo4j.data.Neo4jGraph;
import org.evomaster.client.java.controller.neo4j.data.Neo4jNode;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reads the live Neo4j graph into an in-memory {@link Neo4jGraph} snapshot, used as {@code G} when
 * scoring Cypher queries. The whole graph is read with two read-only Cypher queries that return only
 * primitive projections (ids, label/type names, property maps).
 * <p>
 * All driver access goes through {@link ReflectionBasedNeo4jClient}, so this class holds no
 * reflection of its own and is only about turning records into the graph model.
 */
public class Neo4jGraphReader {

    private static final String NODE_QUERY =
            "MATCH (n) RETURN elementId(n) AS id, labels(n) AS labels, properties(n) AS props";

    private static final String REL_QUERY =
            "MATCH ()-[r]->() RETURN elementId(r) AS id, type(r) AS type, "
                    + "elementId(startNode(r)) AS src, elementId(endNode(r)) AS tgt, properties(r) AS props";

    /**
     * Reads all nodes and relationships from the database reachable through {@code driver}.
     *
     * @param driver the SUT's {@code org.neo4j.driver.Driver}, as an {@code Object}
     * @return the in-memory graph snapshot
     * @throws RuntimeException if the driver cannot be queried (wrapping the reflection failure)
     */
    public Neo4jGraph read(Object driver) {
        return read(new ReflectionBasedNeo4jClient(driver));
    }

    /**
     * Reads the graph through an already-built client. Kept separate from {@link #read(Object)} so a
     * caller that holds a client (the handler, or the insertion runner) does not create a second one.
     */
    public Neo4jGraph read(ReflectionBasedNeo4jClient client) {
        Object session = client.session();
        try {
            List<Neo4jNode> nodes = readNodes(client, session);
            List<Neo4jEdge> edges = readEdges(client, session);
            return new Neo4jGraph(nodes, edges);
        } finally {
            client.close(session);
        }
    }

    private List<Neo4jNode> readNodes(ReflectionBasedNeo4jClient client, Object session) {
        List<Neo4jNode> nodes = new ArrayList<>();
        for (Object record : client.runAndList(session, NODE_QUERY)) {
            String id = client.asString(client.get(record, "id"));
            Set<String> labels = toStringSet(client.asList(client.get(record, "labels")));
            Map<String, Object> props = client.asMap(client.get(record, "props"));
            nodes.add(new Neo4jNode(id, labels, props));
        }
        return nodes;
    }

    private List<Neo4jEdge> readEdges(ReflectionBasedNeo4jClient client, Object session) {
        List<Neo4jEdge> edges = new ArrayList<>();
        for (Object record : client.runAndList(session, REL_QUERY)) {
            String id = client.asString(client.get(record, "id"));
            String type = client.asString(client.get(record, "type"));
            String src = client.asString(client.get(record, "src"));
            String tgt = client.asString(client.get(record, "tgt"));
            Map<String, Object> props = client.asMap(client.get(record, "props"));
            edges.add(new Neo4jEdge(id, type, src, tgt, props));
        }
        return edges;
    }

    private Set<String> toStringSet(List<?> values) {
        Set<String> set = new LinkedHashSet<>();
        for (Object v : values) {
            set.add(String.valueOf(v));
        }
        return set;
    }
}
