package org.evomaster.client.java.controller.internal.db.neo4j;

import org.evomaster.client.java.controller.neo4j.data.Neo4jEdge;
import org.evomaster.client.java.controller.neo4j.data.Neo4jGraph;
import org.evomaster.client.java.controller.neo4j.data.Neo4jNode;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reads the live Neo4j graph into an in-memory {@link Neo4jGraph} snapshot, used as {@code G} when
 * scoring Cypher queries. The whole graph is read with two read-only Cypher queries that return only
 * primitive projections (ids, label/type names, property maps), so the reflection surface is just
 * {@code Session.run} / {@code Result.list} / {@code Record.get} / {@code Value.as*} — never the
 * driver's {@code Node}/{@code Relationship} types.
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
        Object session = invoke(driver, "session");
        try {
            List<Neo4jNode> nodes = readNodes(session);
            List<Neo4jEdge> edges = readEdges(session);
            return new Neo4jGraph(nodes, edges);
        } finally {
            invoke(session, "close");
        }
    }

    private List<Neo4jNode> readNodes(Object session) {
        List<Neo4jNode> nodes = new ArrayList<>();
        for (Object record : runAndList(session, NODE_QUERY)) {
            String id = asString(get(record, "id"));
            Set<String> labels = toStringSet(asList(get(record, "labels")));
            Map<String, Object> props = asMap(get(record, "props"));
            nodes.add(new Neo4jNode(id, labels, props));
        }
        return nodes;
    }

    private List<Neo4jEdge> readEdges(Object session) {
        List<Neo4jEdge> edges = new ArrayList<>();
        for (Object record : runAndList(session, REL_QUERY)) {
            String id = asString(get(record, "id"));
            String type = asString(get(record, "type"));
            String src = asString(get(record, "src"));
            String tgt = asString(get(record, "tgt"));
            Map<String, Object> props = asMap(get(record, "props"));
            edges.add(new Neo4jEdge(id, type, src, tgt, props));
        }
        return edges;
    }

    private List<?> runAndList(Object session, String query) {
        Object result = invoke(session, "run", new Class<?>[]{String.class}, query);
        return (List<?>) invoke(result, "list");
    }

    private Object get(Object record, String key) {
        return invoke(record, "get", new Class<?>[]{String.class}, key);
    }

    private String asString(Object value) {
        return (String) invoke(value, "asString");
    }

    private List<?> asList(Object value) {
        return (List<?>) invoke(value, "asList");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        return (Map<String, Object>) invoke(value, "asMap");
    }

    private Set<String> toStringSet(List<?> values) {
        Set<String> set = new LinkedHashSet<>();
        for (Object v : values) {
            set.add(String.valueOf(v));
        }
        return set;
    }

    private Object invoke(Object target, String method) {
        return invoke(target, method, new Class<?>[0]);
    }

    private Object invoke(Object target, String method, Class<?>[] argTypes, Object... args) {
        try {
            Method m = target.getClass().getMethod(method, argTypes);
            m.setAccessible(true);
            return m.invoke(target, args);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to read the Neo4j graph via reflection (" + method + ")", e);
        }
    }
}
