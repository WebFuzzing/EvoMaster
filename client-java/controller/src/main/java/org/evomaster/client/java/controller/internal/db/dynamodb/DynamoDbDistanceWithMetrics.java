package org.evomaster.client.java.controller.internal.db.dynamodb;

/**
 * Result of evaluating one DynamoDB predicate against the items of one table.
 */
public final class DynamoDbDistanceWithMetrics {

    private final double distance;
    private final int numberOfEvaluatedItems;
    private final boolean evaluationFailure;

    /**
     * Creates a DynamoDB heuristic result.
     *
     * @param distance normalized distance to satisfying the predicate
     * @param numberOfEvaluatedItems number of table items considered
     * @param evaluationFailure whether the evaluation failed
     */
    public DynamoDbDistanceWithMetrics(double distance, int numberOfEvaluatedItems, boolean evaluationFailure) {
        if (distance < 0.0d || distance > 1.0d || Double.isNaN(distance)) {
            throw new IllegalArgumentException("distance must be between 0 and 1, but was " + distance);
        }
        if (numberOfEvaluatedItems < 0) {
            throw new IllegalArgumentException("numberOfEvaluatedItems must be non-negative");
        }
        this.distance = distance;
        this.numberOfEvaluatedItems = numberOfEvaluatedItems;
        this.evaluationFailure = evaluationFailure;
    }

    /**
     * @return normalized distance to satisfying the predicate
     */
    public double getDistance() {
        return distance;
    }

    /**
     * @return number of table items considered
     */
    public int getNumberOfEvaluatedItems() {
        return numberOfEvaluatedItems;
    }

    /**
     * @return whether the evaluation failed
     */
    public boolean isEvaluationFailure() {
        return evaluationFailure;
    }
}
