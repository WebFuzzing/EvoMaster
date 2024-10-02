package org.evomaster.client.java.controller.internal.db;

public class MongoCommandWithDistance {

    public final Object mongoCommand;

    public final MongoDistanceWithMetrics mongoDistanceWithMetrics;

    public MongoCommandWithDistance(Object mongoCommand, MongoDistanceWithMetrics mongoDistanceWithMetrics) {
        this.mongoCommand = mongoCommand;
        this.mongoDistanceWithMetrics = mongoDistanceWithMetrics;
    }
}
