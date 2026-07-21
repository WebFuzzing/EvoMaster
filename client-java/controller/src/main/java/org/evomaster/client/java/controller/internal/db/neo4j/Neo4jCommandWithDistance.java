package org.evomaster.client.java.controller.internal.db.neo4j;

/**
 * Pairs a captured Cypher query with its computed distance to being satisfied by the live graph.
 */
public class Neo4jCommandWithDistance {

    private final String neo4jCommand;

    private final Neo4jDistanceWithMetrics neo4jDistanceWithMetrics;

    public Neo4jCommandWithDistance(String neo4jCommand, Neo4jDistanceWithMetrics neo4jDistanceWithMetrics) {
        this.neo4jCommand = neo4jCommand;
        this.neo4jDistanceWithMetrics = neo4jDistanceWithMetrics;
    }

    public String getNeo4jCommand() {
        return neo4jCommand;
    }

    public Neo4jDistanceWithMetrics getNeo4jDistanceWithMetrics() {
        return neo4jDistanceWithMetrics;
    }
}
