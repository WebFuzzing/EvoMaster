package org.evomaster.client.java.controller.internal.db;

import org.evomaster.client.java.instrumentation.RedisCommand;

/**
 * This class will link a given RedisCommand to the result of the distance calculation for that commmand.
 */
public class RedisCommandEvaluation {
    public final RedisCommand redisCommand;
    public final RedisDistanceWithMetrics redisDistanceWithMetrics;

    public RedisCommandEvaluation(RedisCommand redisCommand, RedisDistanceWithMetrics redisDistanceWithMetrics) {
        this.redisCommand = redisCommand;
        this.redisDistanceWithMetrics = redisDistanceWithMetrics;
    }
}
