package org.evomaster.client.java.controller.internal.db.redis;

/**
 * This class will have the distance for a RedisCommand (between 0 and 1)
 * and the number of evaluated keys in that distance calculation.
 */
public class RedisDistanceWithMetrics {
    private final double redisDistance; // A number between 0 and 1.
    private final int numberOfEvaluatedKeys;

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
