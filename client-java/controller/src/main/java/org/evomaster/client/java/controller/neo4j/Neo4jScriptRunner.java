package org.evomaster.client.java.controller.neo4j;

import org.evomaster.client.java.controller.api.dto.database.operations.Neo4jDatabaseCommandsDto;
import org.evomaster.client.java.controller.api.dto.database.operations.Neo4jEdgeInsertionDto;
import org.evomaster.client.java.controller.api.dto.database.operations.Neo4jInsertionEntryDto;
import org.evomaster.client.java.controller.api.dto.database.operations.Neo4jInsertionResultsDto;
import org.evomaster.client.java.controller.api.dto.database.operations.Neo4jNodeInsertionDto;
import org.evomaster.client.java.utils.SimpleLogger;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Class used to execute Neo4j insertions.
 */
public class Neo4jScriptRunner {

    /** Alias of the column through which a created element reports its {@code elementId}. */
    private static final String ID_FIELD = "id";

    /** Name of the query parameter holding the property map of the element to create. */
    private static final String PROPS_PARAMETER = "props";

    /** Name of the query parameter holding the {@code elementId} of the node a relationship starts from. */
    private static final String FROM_PARAMETER = "from";

    /** Name of the query parameter holding the {@code elementId} of the node a relationship points to. */
    private static final String TO_PARAMETER = "to";

    /**
     * Default constructor
     */
    public Neo4jScriptRunner() {}

    /**
     * Inserts every node and then every relationship. The {@code elementId} Neo4j gives each node is
     * kept, so a relationship can name its endpoints through the ids of their insertions.
     * <p>
     * An insertion that fails is logged and marked as failed in the returned results, and the
     * execution carries on with the next one. A relationship fails when either of its endpoints did.
     * <p>
     * Property values travel as query parameters. Labels and relationship types cannot, as Cypher has
     * no parameter syntax for them, so they are written into the query between backticks.
     *
     * @param client client over the SUT's Neo4j driver
     * @param commands the nodes and relationships to insert
     * @return for each insertion, whether it executed, plus the {@code elementId} of each inserted node
     * @throws IllegalArgumentException if there is nothing to insert
     */
    public static Neo4jInsertionResultsDto executeInsert(ReflectionBasedNeo4jClient client,
                                                         Neo4jDatabaseCommandsDto commands) {

        if (commands == null || (isEmpty(commands.nodes) && isEmpty(commands.edges))) {
            throw new IllegalArgumentException("No data to insert");
        }

        Neo4jInsertionResultsDto results = new Neo4jInsertionResultsDto();

        Object session = client.session();
        try {
            if (commands.nodes != null) {
                for (int i = 0; i < commands.nodes.size(); i++) {
                    results.nodeExecutionResults.add(
                            insertNode(client, session, commands.nodes.get(i), i, results.idMapping));
                }
            }
            if (commands.edges != null) {
                for (int i = 0; i < commands.edges.size(); i++) {
                    results.edgeExecutionResults.add(
                            insertEdge(client, session, commands.edges.get(i), i, results.idMapping));
                }
            }
        } finally {
            client.close(session);
        }

        return results;
    }

    private static boolean insertNode(ReflectionBasedNeo4jClient client, Object session,
                                      Neo4jNodeInsertionDto node, int index, Map<Long, String> idMapping) {
        try {
            if (node.id == null) {
                throw new IllegalArgumentException("Node with no id");
            }
            if (idMapping.containsKey(node.id)) {
                throw new IllegalArgumentException("Repeated node id: " + node.id);
            }

            StringBuilder cypher = new StringBuilder("CREATE (n");
            if (node.labels != null) {
                for (String label : node.labels) {
                    cypher.append(':').append(escapeName(label));
                }
            }
            cypher.append(" $").append(PROPS_PARAMETER).append(") RETURN elementId(n) AS ").append(ID_FIELD);

            Map<String, Object> parameters = new LinkedHashMap<>();
            parameters.put(PROPS_PARAMETER, toPropertyMap(node.properties));

            idMapping.put(node.id, createAndGetId(client, session, cypher.toString(), parameters));
            return true;
        } catch (Exception e) {
            SimpleLogger.warn("Failed to insert Neo4j node at index " + index + ": " + e.getMessage());
            return false;
        }
    }

    private static boolean insertEdge(ReflectionBasedNeo4jClient client, Object session,
                                      Neo4jEdgeInsertionDto edge, int index, Map<Long, String> idMapping) {
        try {
            String from = idMapping.get(edge.fromNodeId);
            String to = idMapping.get(edge.toNodeId);
            if (from == null || to == null) {
                throw new IllegalArgumentException("It connects a node that was not inserted: "
                        + (from == null ? edge.fromNodeId : edge.toNodeId));
            }

            String cypher = "MATCH (a), (b) WHERE elementId(a) = $" + FROM_PARAMETER
                    + " AND elementId(b) = $" + TO_PARAMETER
                    + " CREATE (a)-[r:" + escapeName(edge.type) + " $" + PROPS_PARAMETER + "]->(b)"
                    + " RETURN elementId(r) AS " + ID_FIELD;

            Map<String, Object> parameters = new LinkedHashMap<>();
            parameters.put(FROM_PARAMETER, from);
            parameters.put(TO_PARAMETER, to);
            parameters.put(PROPS_PARAMETER, toPropertyMap(edge.properties));

            createAndGetId(client, session, cypher, parameters);
            return true;
        } catch (Exception e) {
            SimpleLogger.warn("Failed to insert Neo4j relationship at index " + index + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * @return the {@code elementId} of the single element the query created
     */
    private static String createAndGetId(ReflectionBasedNeo4jClient client, Object session,
                                         String cypher, Map<String, Object> parameters) {
        List<?> records = client.runAndList(session, cypher, parameters);
        if (records.size() != 1) {
            throw new IllegalStateException("Expected 1 created element but got " + records.size());
        }
        return client.asString(client.get(records.get(0), ID_FIELD));
    }

    /**
     * @return the properties by name, each value in the Java type its declared type maps to
     */
    private static Map<String, Object> toPropertyMap(List<Neo4jInsertionEntryDto> properties) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (properties == null) {
            return map;
        }
        for (Neo4jInsertionEntryDto entry : properties) {
            if (entry.propertyKey == null || entry.propertyKey.isEmpty()) {
                throw new IllegalArgumentException("Property with no key");
            }
            map.put(entry.propertyKey, toValue(entry));
        }
        return map;
    }

    private static Object toValue(Neo4jInsertionEntryDto entry) {
        if (entry.type == null || entry.value == null) {
            throw new IllegalArgumentException("Property with no type or no value: " + entry.propertyKey);
        }
        switch (entry.type) {
            case STRING:
                return entry.value;
            case INTEGER:
                return Long.valueOf(entry.value);
            case FLOAT:
                return Double.valueOf(entry.value);
            case BOOLEAN:
                if ("true".equalsIgnoreCase(entry.value) || "false".equalsIgnoreCase(entry.value)) {
                    return Boolean.parseBoolean(entry.value);
                }
                throw new IllegalArgumentException("Not a boolean: " + entry.value);
            default:
                throw new IllegalArgumentException("Not supported property type: " + entry.type);
        }
    }

    /**
     * Quotes a label or a relationship type with backticks, doubling any backtick inside it, so that
     * whatever the name holds is read as a name and never as Cypher.
     */
    private static String escapeName(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Label or relationship type with no name");
        }
        return '`' + name.replace("`", "``") + '`';
    }

    private static boolean isEmpty(List<?> list) {
        return list == null || list.isEmpty();
    }
}
