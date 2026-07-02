package org.evomaster.client.java.controller.internal.db.neo4j;

/**
 * Pairs a captured Cypher query with its computed distance to being satisfied by the live graph.
 */
public class Neo4jCommandWithDistance {

    public final String neo4jCommand;

    public final Neo4jDistanceWithMetrics neo4jDistanceWithMetrics;

    public Neo4jCommandWithDistance(String neo4jCommand, Neo4jDistanceWithMetrics neo4jDistanceWithMetrics) {
        this.neo4jCommand = neo4jCommand;
        this.neo4jDistanceWithMetrics = neo4jDistanceWithMetrics;
    }
}
