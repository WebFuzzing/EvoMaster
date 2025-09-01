package org.evomaster.client.java.controller.internal.db;

public class OpenSearchDistanceWithMetrics {

    private final double distance;

    private final int numberOfEvaluatedDocuments;

    public OpenSearchDistanceWithMetrics(double distance, int numberOfEvaluatedDocuments) {
        if(distance < 0){
            throw new IllegalArgumentException("distance must be non-negative but value is " + distance);
        }
        if(numberOfEvaluatedDocuments < 0){
            throw new IllegalArgumentException("numberOfEvaluatedDocuments must be non-negative but value is " + numberOfEvaluatedDocuments);
        }
        this.distance = distance;
        this.numberOfEvaluatedDocuments = numberOfEvaluatedDocuments;
    }

    public double getDistance() {
        return distance;
    }

    public int getNumberOfEvaluatedDocuments() {
        return numberOfEvaluatedDocuments;
    }
}
