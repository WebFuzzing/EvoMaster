package org.evomaster.client.java.controller.internal.db;

public class MongoDistanceWithMetrics {

    public final double findDistance;

    public final int numberOfEvaluatedDocuments;

    public MongoDistanceWithMetrics(double findDistance, int numberOfEvaluatedDocuments) {
        this.findDistance = findDistance;
        this.numberOfEvaluatedDocuments = numberOfEvaluatedDocuments;
    }
}
