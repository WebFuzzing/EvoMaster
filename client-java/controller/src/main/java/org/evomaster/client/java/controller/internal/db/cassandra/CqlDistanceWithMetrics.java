package org.evomaster.client.java.controller.internal.db.cassandra;

import java.util.Objects;

/**
 * The heuristic distance computed for a single CQL command, plus the number of rows that were
 * fetched from the target table in order to compute it.
 */
public class CqlDistanceWithMetrics {

    private final double cqlDistance;

    private final int numberOfEvaluatedRows;

    public CqlDistanceWithMetrics(double cqlDistance, int numberOfEvaluatedRows) {
        Objects.requireNonNull(cqlDistance);
        Objects.requireNonNull(numberOfEvaluatedRows);

        if (cqlDistance < 0) {
            throw new IllegalArgumentException("cqlDistance must be non-negative but value is " + cqlDistance);
        }
        if (numberOfEvaluatedRows < 0) {
            throw new IllegalArgumentException("numberOfEvaluatedRows must be non-negative but value is " + numberOfEvaluatedRows);
        }
        this.cqlDistance = cqlDistance;
        this.numberOfEvaluatedRows = numberOfEvaluatedRows;
    }

    public double getCqlDistance() {
        return cqlDistance;
    }

    public int getNumberOfEvaluatedRows() {
        return numberOfEvaluatedRows;
    }
}