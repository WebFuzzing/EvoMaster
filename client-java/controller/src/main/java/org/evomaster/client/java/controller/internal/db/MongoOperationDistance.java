package org.evomaster.client.java.controller.internal.db;

public class MongoOperationDistance {

    public final Object findQuery;

    public final double findDistance;

    public final int numberOfEvaluatedDocuments;

    public MongoOperationDistance(Object findQuery, double findDistance, int numberOfEvaluatedDocuments) {
        this.findQuery = findQuery;
        this.findDistance = findDistance;
        this.numberOfEvaluatedDocuments = numberOfEvaluatedDocuments;
    }
}
