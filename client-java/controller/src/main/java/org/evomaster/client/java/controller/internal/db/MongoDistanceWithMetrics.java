package org.evomaster.client.java.controller.internal.db;

public class MongoDistanceWithMetrics {

    public final double mongoDistance;

    public final int numberOfEvaluatedDocuments;

    public MongoDistanceWithMetrics(double mongoDistance, int numberOfEvaluatedDocuments) {
        if(mongoDistance < 0){
            throw new IllegalArgumentException("mongoDistance must be non-negative but value is " + mongoDistance);
        }
        if(numberOfEvaluatedDocuments < 0){
            throw new IllegalArgumentException("numberOfEvaluatedDocuments must be non-negative but value is " + numberOfEvaluatedDocuments);
        }
        this.mongoDistance = mongoDistance;
        this.numberOfEvaluatedDocuments = numberOfEvaluatedDocuments;
    }
}
