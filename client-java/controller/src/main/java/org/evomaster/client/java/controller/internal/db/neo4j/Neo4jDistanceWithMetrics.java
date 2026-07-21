package org.evomaster.client.java.controller.internal.db.neo4j;

/**
 * The result of scoring one captured Cypher query against the live graph: the distance to satisfying
 * it ({@code 1 - ofTrue}, in {@code [0,1]}, 0 meaning satisfied), how many graph nodes were available
 * when scoring, and whether the evaluation failed (e.g. the query could not be parsed).
 */
public class Neo4jDistanceWithMetrics {

    private final double neo4jDistance;

    private final int numberOfEvaluatedNodes;

    private final boolean neo4jDistanceEvaluationFailure;

    public Neo4jDistanceWithMetrics(double neo4jDistance, int numberOfEvaluatedNodes,
                                    boolean neo4jDistanceEvaluationFailure) {
        if (neo4jDistance < 0) {
            throw new IllegalArgumentException("neo4jDistance must be non-negative but value is " + neo4jDistance);
        }
        if (numberOfEvaluatedNodes < 0) {
            throw new IllegalArgumentException(
                    "numberOfEvaluatedNodes must be non-negative but value is " + numberOfEvaluatedNodes);
        }
        this.neo4jDistance = neo4jDistance;
        this.numberOfEvaluatedNodes = numberOfEvaluatedNodes;
        this.neo4jDistanceEvaluationFailure = neo4jDistanceEvaluationFailure;
    }

    public double getNeo4jDistance() {
        return neo4jDistance;
    }

    public int getNumberOfEvaluatedNodes() {
        return numberOfEvaluatedNodes;
    }

    public boolean isNeo4jDistanceEvaluationFailure() {
        return neo4jDistanceEvaluationFailure;
    }
}
