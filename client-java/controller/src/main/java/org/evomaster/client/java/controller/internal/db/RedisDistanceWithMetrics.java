package org.evomaster.client.java.controller.internal.db;

public class RedisDistanceWithMetrics {
    public final double redisDistance; // A number between 0 and 1.
    public final int numberOfEvaluatedKeys;

    public RedisDistanceWithMetrics(double redisDistance, int numberOfEvaluatedKeys) {
        if(redisDistance < 0){
            throw new IllegalArgumentException("Distance must be non-negative but value is " + redisDistance);
        }
        if(numberOfEvaluatedKeys < 0){
            throw new IllegalArgumentException("Number of evaluated keys must be non-negative but value is "
                    + numberOfEvaluatedKeys);
        }
        this.redisDistance = redisDistance;
        this.numberOfEvaluatedKeys = numberOfEvaluatedKeys;
    }

    public int getNumberOfEvaluatedKeys() {
        return numberOfEvaluatedKeys;
    }

    public double getDistance() {
        return redisDistance;
    }
}
