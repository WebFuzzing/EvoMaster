package org.evomaster.client.java.controller.internal.db.cassandra;

/**
 * Pairs a CQL command with its computed heuristic distance.
 */
public class CqlCommandWithDistance {

    public final String cqlCommand;

    public final CqlDistanceWithMetrics cqlDistanceWithMetrics;

    public CqlCommandWithDistance(String cqlCommand, CqlDistanceWithMetrics cqlDistanceWithMetrics) {
        this.cqlCommand = cqlCommand;
        this.cqlDistanceWithMetrics = cqlDistanceWithMetrics;
    }
}