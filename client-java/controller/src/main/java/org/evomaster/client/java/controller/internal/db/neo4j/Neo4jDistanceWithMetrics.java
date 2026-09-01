package org.evomaster.client.java.controller.internal.db.neo4j;

/**
 * Result of scoring one captured Cypher query against the live graph.
 */
public final class Neo4jDistanceWithMetrics {

    private final double distance;
    private final int numberOfEvaluatedNodes;
    private final boolean evaluationFailure;

    /**
     * Creates a Neo4j heuristic result.
     *
     * @param distance normalized distance to satisfying the query, 0 meaning satisfied
     * @param numberOfEvaluatedNodes number of graph nodes considered
     * @param evaluationFailure whether the evaluation failed
     */
    public Neo4jDistanceWithMetrics(double distance, int numberOfEvaluatedNodes, boolean evaluationFailure) {
        if (distance < 0.0d || distance > 1.0d || Double.isNaN(distance)) {
            throw new IllegalArgumentException("distance must be between 0 and 1, but was " + distance);
        }
        if (numberOfEvaluatedNodes < 0) {
            throw new IllegalArgumentException("numberOfEvaluatedNodes must be non-negative");
        }
        this.distance = distance;
        this.numberOfEvaluatedNodes = numberOfEvaluatedNodes;
        this.evaluationFailure = evaluationFailure;
    }

    /**
     * @return normalized distance to satisfying the query, 0 meaning satisfied
     */
    public double getDistance() {
        return distance;
    }

    /**
     * @return number of graph nodes considered
     */
    public int getNumberOfEvaluatedNodes() {
        return numberOfEvaluatedNodes;
    }

    /**
     * @return whether the evaluation failed
     */
    public boolean isEvaluationFailure() {
        return evaluationFailure;
    }
}
