package org.evomaster.client.java.controller.internal.db.redis;

import org.evomaster.client.java.instrumentation.RedisCommand;

/**
 * This class will link a given RedisCommand to the result of the distance calculation for that commmand.
 */
public class RedisCommandEvaluation {
    private final RedisCommand redisCommand;
    private final RedisDistanceWithMetrics redisDistanceWithMetrics;

    public RedisCommandEvaluation(RedisCommand redisCommand, RedisDistanceWithMetrics redisDistanceWithMetrics) {
        this.redisCommand = redisCommand;
        this.redisDistanceWithMetrics = redisDistanceWithMetrics;
    }

    public RedisCommand getRedisCommand() {
        return redisCommand;
    }

    public RedisDistanceWithMetrics getRedisDistanceWithMetrics() {
        return redisDistanceWithMetrics;
    }
}
