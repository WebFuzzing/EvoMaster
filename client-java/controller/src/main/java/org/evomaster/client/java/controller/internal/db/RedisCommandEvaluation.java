package org.evomaster.client.java.controller.internal.db;

import org.evomaster.client.java.instrumentation.RedisCommand;

public class RedisCommandEvaluation {
    public final RedisCommand redisCommand;
    public final RedisDistanceWithMetrics redisDistanceWithMetrics;

    public RedisCommandEvaluation(RedisCommand redisCommand, RedisDistanceWithMetrics redisDistanceWithMetrics) {
        this.redisCommand = redisCommand;
        this.redisDistanceWithMetrics = redisDistanceWithMetrics;
    }
}
