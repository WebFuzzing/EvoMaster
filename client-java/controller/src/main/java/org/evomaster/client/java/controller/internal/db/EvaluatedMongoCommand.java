package org.evomaster.client.java.controller.internal.db;

public class EvaluatedMongoCommand {

    public final Object mongoCommand;

    public final MongoDistanceWithMetrics mongoDistanceWithMetrics;

    public EvaluatedMongoCommand(Object mongoCommand, MongoDistanceWithMetrics mongoDistanceWithMetrics) {
        this.mongoCommand = mongoCommand;
        this.mongoDistanceWithMetrics = mongoDistanceWithMetrics;
    }
}
