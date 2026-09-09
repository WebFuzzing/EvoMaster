package org.evomaster.client.java.controller.internal.db.neo4j;

import org.evomaster.client.java.controller.neo4j.heuristics.Neo4jHeuristicsCalculator;

/**
 * Result of scoring one captured Cypher query against the live graph: the distance to satisfying the
 * query, how many graph nodes were considered, and whether the evaluation failed.
 * <p>
 * The distance is {@code 1 - ofTrue} of the {@code Truthness} computed by the heuristics calculator, so a
 * computed value lies in {@code [0,1]} by construction, with 0 meaning the query is satisfied. A failed
 * evaluation carries {@link Neo4jHeuristicsCalculator#MAX_NEO4J_DISTANCE}.
 */
public final class Neo4jDistanceWithMetrics {

    private final double distance;
    private final int numberOfEvaluatedNodes;
    private final boolean evaluationFailure;

    /**
     * Creates a Neo4j heuristic result.
     *
     * @param distance distance to satisfying the query, 0 meaning satisfied
     * @param numberOfEvaluatedNodes number of graph nodes considered
     * @param evaluationFailure whether the evaluation failed, in which case the distance must be
     *                          {@link Neo4jHeuristicsCalculator#MAX_NEO4J_DISTANCE}
     */
    public Neo4jDistanceWithMetrics(double distance, int numberOfEvaluatedNodes, boolean evaluationFailure) {
        if (distance < 0.0d || Double.isNaN(distance)) {
            throw new IllegalArgumentException("distance must be non-negative, but was " + distance);
        }
        if (numberOfEvaluatedNodes < 0) {
            throw new IllegalArgumentException("numberOfEvaluatedNodes must be non-negative");
        }
        if (evaluationFailure && distance != Neo4jHeuristicsCalculator.MAX_NEO4J_DISTANCE) {
            throw new IllegalArgumentException(
                    "a failed Neo4j distance computation cannot have a value different than MAX_NEO4J_DISTANCE");
        }
        this.distance = distance;
        this.numberOfEvaluatedNodes = numberOfEvaluatedNodes;
        this.evaluationFailure = evaluationFailure;
    }

    /**
     * @return distance to satisfying the query, 0 meaning satisfied,
     * {@link Neo4jHeuristicsCalculator#MAX_NEO4J_DISTANCE} on failure
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
