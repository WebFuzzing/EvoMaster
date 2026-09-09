package org.evomaster.client.java.controller.internal.db.neo4j;

import org.evomaster.client.java.controller.neo4j.heuristics.Neo4jHeuristicsCalculator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Neo4jDistanceWithMetricsTest {

    @Test
    void testComputedDistanceIsKeptAsIs() {
        Neo4jDistanceWithMetrics metrics = new Neo4jDistanceWithMetrics(0.25d, 3, false);
        assertEquals(0.25d, metrics.getDistance(), 0.0d);
        assertEquals(3, metrics.getNumberOfEvaluatedNodes());
        assertFalse(metrics.isEvaluationFailure());
    }

    @Test
    void testNegativeDistanceIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Neo4jDistanceWithMetrics(-0.1d, 0, false));
    }

    @Test
    void testFailureMustCarryTheMaximumDistance() {
        assertThrows(IllegalArgumentException.class, () -> new Neo4jDistanceWithMetrics(1.0d, 0, true));
        Neo4jDistanceWithMetrics failed =
                new Neo4jDistanceWithMetrics(Neo4jHeuristicsCalculator.MAX_NEO4J_DISTANCE, 0, true);
        assertTrue(failed.isEvaluationFailure());
        assertEquals(Double.MAX_VALUE, failed.getDistance(), 0.0d);
    }
}
