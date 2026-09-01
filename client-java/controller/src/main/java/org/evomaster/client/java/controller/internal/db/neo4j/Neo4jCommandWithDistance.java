package org.evomaster.client.java.controller.internal.db.neo4j;

/**
 * Pairs a captured Cypher query with its computed distance to being satisfied by the live graph.
 */
public final class Neo4jCommandWithDistance {

    private final String command;
    private final Neo4jDistanceWithMetrics distanceWithMetrics;

    /**
     * Creates the evaluation of one captured query.
     *
     * @param command the Cypher query, as executed by the SUT
     * @param distanceWithMetrics its heuristic result
     */
    public Neo4jCommandWithDistance(String command, Neo4jDistanceWithMetrics distanceWithMetrics) {
        this.command = command;
        this.distanceWithMetrics = distanceWithMetrics;
    }

    /**
     * @return the Cypher query, as executed by the SUT
     */
    public String getCommand() {
        return command;
    }

    /**
     * @return the heuristic result of the query
     */
    public Neo4jDistanceWithMetrics getDistanceWithMetrics() {
        return distanceWithMetrics;
    }
}
