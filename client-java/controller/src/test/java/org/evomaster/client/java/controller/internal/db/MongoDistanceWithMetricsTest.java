package org.evomaster.client.java.controller.internal.db;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class MongoDistanceWithMetricsTest {

    @Test
    public void testNegativeMongoDistance() {
        assertThrows(IllegalArgumentException.class, () ->
            new MongoDistanceWithMetrics(-1.0, 0)
        );
    }

    @Test
    public void testNegativeNumberOfDocuments() {
        assertThrows(IllegalArgumentException.class, () ->
            new MongoDistanceWithMetrics(1.0, -1)
        );
    }

}
