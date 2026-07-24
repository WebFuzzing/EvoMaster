package org.evomaster.client.java.controller.internal.db.cassandra;

/**
 * The heuristic distance computed for a single CQL command, plus the number of rows that were
 * fetched from the target table in order to compute it.
 */
public class CqlDistanceWithMetrics {

    public final double cqlDistance;

    public final int numberOfEvaluatedRows;

    public CqlDistanceWithMetrics(double cqlDistance, int numberOfEvaluatedRows) {
        if (cqlDistance < 0) {
            throw new IllegalArgumentException("cqlDistance must be non-negative but value is " + cqlDistance);
        }
        if (numberOfEvaluatedRows < 0) {
            throw new IllegalArgumentException("numberOfEvaluatedRows must be non-negative but value is " + numberOfEvaluatedRows);
        }
        this.cqlDistance = cqlDistance;
        this.numberOfEvaluatedRows = numberOfEvaluatedRows;
    }
}