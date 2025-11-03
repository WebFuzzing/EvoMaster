package org.evomaster.client.java.controller.internal.db.mongo;

/**
 * A simple data class to hold the mongo heuristic distance along with
 * metrics regarding the cost of computing such distance (e.g., number of documents evaluated).
 * This distance is used as a secondary objective during search-based test generation.
 */
public class MongoDistanceWithMetrics {

    /**
     * The computed MongoDB heuristic distance.
     * The value is a number between 0 (exact match) and
     * Double.MAX_VALUE (completely different).
     */
    public final double mongoDistance;

    /**
     * The number of documents evaluated to compute the Mongo
     * heuristic distance.
     * The number of documents evaluated is always non-negative.
     */
    public final int numberOfEvaluatedDocuments;

    /**
     * Creates a new instance of MongoDistanceWithMetrics.
     *
     * @param mongoDistance a non-negative double representing the MongoDB heuristic distance
     * @param numberOfEvaluatedDocuments a non-negative integer representing the number of documents evaluated
     */
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
