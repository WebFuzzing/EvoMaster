package org.evomaster.client.java.sql.internal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class SqlDistanceWithMetricsTest {
    @Test
    public void testNegativeSqlDistance() {
        assertThrows(IllegalArgumentException.class, ()
                -> new SqlDistanceWithMetrics(-1.0, 0));
    }

    @Test
    public void testNegativeNumberOfRows() {
        assertThrows(IllegalArgumentException.class, () ->
            new SqlDistanceWithMetrics(1.0, -1));
    }

}