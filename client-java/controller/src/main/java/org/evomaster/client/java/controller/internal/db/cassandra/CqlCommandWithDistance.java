package org.evomaster.client.java.controller.internal.db.cassandra;

import java.util.Objects;

/**
 * Pairs a CQL command with its computed heuristic distance.
 */
public class CqlCommandWithDistance {

    private final String cqlCommand;

    private final CqlDistanceWithMetrics cqlDistanceWithMetrics;

    public CqlCommandWithDistance(String cqlCommand, CqlDistanceWithMetrics cqlDistanceWithMetrics) {
        this.cqlCommand = Objects.requireNonNull(cqlCommand);
        this.cqlDistanceWithMetrics = Objects.requireNonNull(cqlDistanceWithMetrics);
    }

    public String getCqlCommand() {
        return cqlCommand;
    }

    public CqlDistanceWithMetrics getCqlDistanceWithMetrics() {
        return cqlDistanceWithMetrics;
    }
}