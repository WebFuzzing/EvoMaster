package org.evomaster.client.java.controller.internal.db;

import org.evomaster.client.java.controller.internal.db.redis.RedisDistanceWithMetrics;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class RedisDistanceWithMetricsTest {

    @Test
    public void testNegativeRedisDistance() {
        assertThrows(IllegalArgumentException.class, () ->
            new RedisDistanceWithMetrics(-1.0, 0)
        );
    }

    @Test
    public void testNegativeNumberOfDocuments() {
        assertThrows(IllegalArgumentException.class, () ->
            new RedisDistanceWithMetrics(1.0, -1)
        );
    }

}
