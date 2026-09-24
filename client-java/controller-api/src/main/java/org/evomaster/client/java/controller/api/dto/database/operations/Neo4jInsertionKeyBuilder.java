package org.evomaster.client.java.controller.api.dto.database.operations;

import java.util.List;

/**
 * Builds stable identifiers for inferred Neo4j insertions, so that the same insertion is not
 * registered or added twice.
 */
public final class Neo4jInsertionKeyBuilder {

    private Neo4jInsertionKeyBuilder() {
    }

    /**
     * Builds the key of a batch of nodes and relationships from their labels, types, endpoints and
     * property values, in order.
     *
     * @param nodes the nodes of the insertion
     * @param edges the relationships of the insertion
     * @return a key that is equal for two insertions creating the same graph
     */
    public static String fromCommands(List<Neo4jNodeInsertionDto> nodes, List<Neo4jEdgeInsertionDto> edges) {
        StringBuilder key = new StringBuilder();
        for (Neo4jNodeInsertionDto node : nodes) {
            key.append("n").append(node.id);
            for (String label : node.labels) {
                key.append(':').append(label);
            }
            appendProperties(key, node.properties);
            key.append('|');
        }
        for (Neo4jEdgeInsertionDto edge : edges) {
            key.append("e:").append(edge.type).append(' ')
                    .append(edge.fromNodeId).append("->").append(edge.toNodeId);
            appendProperties(key, edge.properties);
            key.append('|');
        }
        return key.toString();
    }

    private static void appendProperties(StringBuilder key, List<Neo4jInsertionEntryDto> properties) {
        key.append('{');
        for (Neo4jInsertionEntryDto property : properties) {
            key.append(property.propertyKey).append(':').append(property.type)
                    .append('=').append(property.value).append(';');
        }
        key.append('}');
    }
}
