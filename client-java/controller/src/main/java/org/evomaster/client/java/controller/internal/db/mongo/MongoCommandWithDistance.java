package org.evomaster.client.java.controller.internal.db.mongo;

/**
 * A simple data class to hold a MongoDB command along with its associated
 * heuristic distance and metrics.
 */
public class MongoCommandWithDistance {

    /** The MongoDB command object. */
    public final Object mongoCommand;

    /** The associated MongoDB heuristic distance with metrics of its calculation. */
    public final MongoDistanceWithMetrics mongoDistanceWithMetrics;

    /**
     * Creates a new instance of MongoCommandWithDistance.
     *
     * @param mongoCommand the MongoDB command object
     * @param mongoDistanceWithMetrics the associated MongoDB heuristic distance with metrics
     */
    public MongoCommandWithDistance(Object mongoCommand, MongoDistanceWithMetrics mongoDistanceWithMetrics) {
        this.mongoCommand = mongoCommand;
        this.mongoDistanceWithMetrics = mongoDistanceWithMetrics;
    }
}
