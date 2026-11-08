package org.evomaster.client.java.controller.neo4j;

import org.evomaster.client.java.controller.internal.db.neo4j.Neo4jGraphReader;
import org.evomaster.client.java.controller.neo4j.data.Neo4jEdge;
import org.evomaster.client.java.controller.neo4j.data.Neo4jGraph;
import org.evomaster.client.java.controller.neo4j.data.Neo4jNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Puts a Neo4j graph back as it was when {@link #takeSnapshot(ReflectionBasedNeo4jClient)} was called,
 * so that a test cannot leave behind data that changes the outcome of the next one.
 * <p>
 * The graph of a SUT is often seeded by the SUT itself when it starts, or by a migration, rather than
 * by a script the driver could run again. So the snapshot is the graph itself, read once, and every
 * {@link #restore(ReflectionBasedNeo4jClient)} only touches what differs from it: nodes and
 * relationships that were not there are deleted, those that were there get their labels and properties
 * back, and those that were deleted are created again. A graph that was not modified is left untouched.
 * <p>
 * Elements are matched by {@code elementId}. Neo4j gives a recreated element a new one, which is why the
 * ids the snapshot elements currently have are tracked across restores.
 */
public class Neo4jGraphRestorer {

    /*
     * Names of the query parameters, and of the fields of each row in a batch. They build the queries as
     * well as the parameter maps, so the name asked for can never drift from the name given.
     */
    private static final String ID_FIELD = "id";
    private static final String IDS_PARAMETER = "ids";
    private static final String ROWS_PARAMETER = "rows";
    private static final String PROPS_PARAMETER = "props";
    private static final String FROM_PARAMETER = "from";
    private static final String TO_PARAMETER = "to";

    private static final String DELETE_RELATIONSHIPS_QUERY = "UNWIND $" + IDS_PARAMETER
            + " AS id MATCH ()-[r]->() WHERE elementId(r) = id DELETE r";

    private static final String DELETE_NODES_QUERY = "UNWIND $" + IDS_PARAMETER
            + " AS id MATCH (n) WHERE elementId(n) = id DETACH DELETE n";

    private static final String SET_NODE_PROPERTIES_QUERY = "UNWIND $" + ROWS_PARAMETER
            + " AS row MATCH (n) WHERE elementId(n) = row." + ID_FIELD + " SET n = row." + PROPS_PARAMETER;

    private static final String SET_RELATIONSHIP_PROPERTIES_QUERY = "UNWIND $" + ROWS_PARAMETER
            + " AS row MATCH ()-[r]->() WHERE elementId(r) = row." + ID_FIELD + " SET r = row." + PROPS_PARAMETER;

    private final Neo4jGraphReader reader = new Neo4jGraphReader();

    /**
     * Key: the {@code elementId} a node had when the snapshot was taken.
     * Value: the node as it was then.
     */
    private Map<String, Neo4jNode> snapshotNodes;

    /**
     * Key: the {@code elementId} a relationship had when the snapshot was taken.
     * Value: the relationship as it was then.
     */
    private Map<String, Neo4jEdge> snapshotEdges;

    /**
     * Key: the {@code elementId} a node had when the snapshot was taken.
     * Value: the {@code elementId} it has now, which differs once it has been recreated.
     */
    private Map<String, String> currentNodeIds;

    /**
     * Key: the {@code elementId} a relationship had when the snapshot was taken.
     * Value: the {@code elementId} it has now, which differs once it has been recreated.
     */
    private Map<String, String> currentEdgeIds;

    /**
     * Reads the whole graph as the state every later restore goes back to, replacing any previous one.
     */
    public void takeSnapshot(ReflectionBasedNeo4jClient client) {
        Neo4jGraph graph = reader.read(client);

        snapshotNodes = new LinkedHashMap<>();
        currentNodeIds = new HashMap<>();
        for (Neo4jNode node : graph.getNodes()) {
            snapshotNodes.put(node.getId(), node);
            currentNodeIds.put(node.getId(), node.getId());
        }

        snapshotEdges = new LinkedHashMap<>();
        currentEdgeIds = new HashMap<>();
        for (Neo4jEdge edge : graph.getEdges()) {
            snapshotEdges.put(edge.getId(), edge);
            currentEdgeIds.put(edge.getId(), edge.getId());
        }
    }

    public boolean hasSnapshot() {
        return snapshotNodes != null;
    }

    /**
     * Forgets the snapshot, so that the next one has to be taken before restoring again.
     */
    public void discardSnapshot() {
        snapshotNodes = null;
        snapshotEdges = null;
        currentNodeIds = null;
        currentEdgeIds = null;
    }

    /**
     * Makes the graph equal to the snapshot again, changing only what differs from it.
     *
     * @throws IllegalStateException if no snapshot was taken
     */
    public void restore(ReflectionBasedNeo4jClient client) {
        if (!hasSnapshot()) {
            throw new IllegalStateException("No snapshot of the Neo4j graph to restore");
        }

        Neo4jGraph graph = reader.read(client);
        Map<String, Neo4jNode> nodesNow = new HashMap<>();
        graph.getNodes().forEach(n -> nodesNow.put(n.getId(), n));
        Map<String, Neo4jEdge> edgesNow = new HashMap<>();
        graph.getEdges().forEach(e -> edgesNow.put(e.getId(), e));

        Object session = client.session();
        try {
            deleteRelationshipsNotInSnapshot(client, session, edgesNow);
            deleteNodesNotInSnapshot(client, session, nodesNow);
            restoreNodes(client, session, nodesNow);
            restoreRelationships(client, session, edgesNow);
        } finally {
            client.close(session);
        }
    }

    /**
     * Deletes the relationships that are not in the snapshot, and also those holding the id of one that
     * is but no longer join the same nodes with the same type (Neo4j may reuse the id of a deleted
     * element). The latter are created again later.
     */
    private void deleteRelationshipsNotInSnapshot(ReflectionBasedNeo4jClient client, Object session,
                                                  Map<String, Neo4jEdge> edgesNow) {
        Set<String> kept = new HashSet<>();
        for (Map.Entry<String, String> e : currentEdgeIds.entrySet()) {
            Neo4jEdge now = edgesNow.get(e.getValue());
            if (now != null && isSameRelationship(snapshotEdges.get(e.getKey()), now)) {
                kept.add(e.getValue());
            }
        }

        List<String> toDelete = new ArrayList<>();
        for (String id : edgesNow.keySet()) {
            if (!kept.contains(id)) {
                toDelete.add(id);
            }
        }
        toDelete.forEach(edgesNow::remove);
        runWithIds(client, session, DELETE_RELATIONSHIPS_QUERY, toDelete);
    }

    private void deleteNodesNotInSnapshot(ReflectionBasedNeo4jClient client, Object session,
                                          Map<String, Neo4jNode> nodesNow) {
        Set<String> kept = new HashSet<>(currentNodeIds.values());
        List<String> toDelete = new ArrayList<>();
        for (String id : nodesNow.keySet()) {
            if (!kept.contains(id)) {
                toDelete.add(id);
            }
        }
        toDelete.forEach(nodesNow::remove);
        runWithIds(client, session, DELETE_NODES_QUERY, toDelete);
    }

    /**
     * Creates again the snapshot nodes that were deleted, and gives the others their labels and
     * properties back where they changed.
     */
    private void restoreNodes(ReflectionBasedNeo4jClient client, Object session, Map<String, Neo4jNode> nodesNow) {
        List<Map<String, Object>> propertyRows = new ArrayList<>();

        for (Neo4jNode original : snapshotNodes.values()) {
            String currentId = currentNodeIds.get(original.getId());
            Neo4jNode now = nodesNow.get(currentId);

            if (now == null) {
                currentNodeIds.put(original.getId(), createNode(client, session, original));
                continue;
            }
            if (!now.getLabels().equals(original.getLabels())) {
                setLabels(client, session, currentId, now.getLabels(), original.getLabels());
            }
            if (!now.getProperties().equals(original.getProperties())) {
                propertyRows.add(row(currentId, original.getProperties()));
            }
        }

        runWithRows(client, session, SET_NODE_PROPERTIES_QUERY, propertyRows);
    }

    /**
     * Creates again the snapshot relationships that were deleted, between the nodes their endpoints are
     * now, and gives the others their properties back where they changed.
     */
    private void restoreRelationships(ReflectionBasedNeo4jClient client, Object session,
                                      Map<String, Neo4jEdge> edgesNow) {
        List<Map<String, Object>> propertyRows = new ArrayList<>();

        for (Neo4jEdge original : snapshotEdges.values()) {
            String currentId = currentEdgeIds.get(original.getId());
            Neo4jEdge now = edgesNow.get(currentId);

            if (now == null) {
                currentEdgeIds.put(original.getId(), createRelationship(client, session, original));
            } else if (!now.getProperties().equals(original.getProperties())) {
                propertyRows.add(row(currentId, original.getProperties()));
            }
        }

        runWithRows(client, session, SET_RELATIONSHIP_PROPERTIES_QUERY, propertyRows);
    }

    /**
     * Whether {@code now} is still the relationship {@code original} of the snapshot: same type, between
     * the nodes its endpoints are now.
     */
    private boolean isSameRelationship(Neo4jEdge original, Neo4jEdge now) {
        return original.getType().equals(now.getType())
                && currentNodeIds.get(original.getSourceId()).equals(now.getSourceId())
                && currentNodeIds.get(original.getTargetId()).equals(now.getTargetId());
    }

    private String createNode(ReflectionBasedNeo4jClient client, Object session, Neo4jNode node) {
        StringBuilder cypher = new StringBuilder("CREATE (n");
        for (String label : node.getLabels()) {
            cypher.append(':').append(Neo4jScriptRunner.escapeName(label));
        }
        cypher.append(" $").append(PROPS_PARAMETER).append(") RETURN elementId(n) AS ").append(ID_FIELD);

        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put(PROPS_PARAMETER, node.getProperties());
        return createAndGetId(client, session, cypher.toString(), parameters);
    }

    private String createRelationship(ReflectionBasedNeo4jClient client, Object session, Neo4jEdge edge) {
        String cypher = "MATCH (a), (b) WHERE elementId(a) = $" + FROM_PARAMETER
                + " AND elementId(b) = $" + TO_PARAMETER
                + " CREATE (a)-[r:" + Neo4jScriptRunner.escapeName(edge.getType()) + " $" + PROPS_PARAMETER + "]->(b)"
                + " RETURN elementId(r) AS " + ID_FIELD;

        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put(FROM_PARAMETER, currentNodeIds.get(edge.getSourceId()));
        parameters.put(TO_PARAMETER, currentNodeIds.get(edge.getTargetId()));
        parameters.put(PROPS_PARAMETER, edge.getProperties());
        return createAndGetId(client, session, cypher, parameters);
    }

    /**
     * Removes the labels a node gained and adds back those it lost. Labels cannot be query parameters,
     * so they are written into the query between backticks.
     */
    private void setLabels(ReflectionBasedNeo4jClient client, Object session, String id,
                           Set<String> now, Set<String> original) {
        StringBuilder cypher = new StringBuilder("MATCH (n) WHERE elementId(n) = $").append(ID_FIELD);
        for (String label : now) {
            if (!original.contains(label)) {
                cypher.append(" REMOVE n:").append(Neo4jScriptRunner.escapeName(label));
            }
        }
        for (String label : original) {
            if (!now.contains(label)) {
                cypher.append(" SET n:").append(Neo4jScriptRunner.escapeName(label));
            }
        }

        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put(ID_FIELD, id);
        client.runAndList(session, cypher.toString(), parameters);
    }

    /**
     * @return the {@code elementId} of the single element the query created
     */
    private String createAndGetId(ReflectionBasedNeo4jClient client, Object session,
                                  String cypher, Map<String, Object> parameters) {
        List<?> records = client.runAndList(session, cypher, parameters);
        if (records.size() != 1) {
            throw new IllegalStateException("Expected 1 created element but got " + records.size());
        }
        return client.asString(client.get(records.get(0), ID_FIELD));
    }

    private void runWithIds(ReflectionBasedNeo4jClient client, Object session, String cypher, Collection<String> ids) {
        if (ids.isEmpty()) {
            return;
        }
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put(IDS_PARAMETER, new ArrayList<>(ids));
        client.runAndList(session, cypher, parameters);
    }

    private void runWithRows(ReflectionBasedNeo4jClient client, Object session, String cypher,
                             List<Map<String, Object>> rows) {
        if (rows.isEmpty()) {
            return;
        }
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put(ROWS_PARAMETER, rows);
        client.runAndList(session, cypher, parameters);
    }

    private Map<String, Object> row(String id, Map<String, Object> properties) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put(ID_FIELD, id);
        row.put(PROPS_PARAMETER, properties);
        return row;
    }
}
