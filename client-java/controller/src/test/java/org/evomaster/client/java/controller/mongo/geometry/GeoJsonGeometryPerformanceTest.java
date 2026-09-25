package org.evomaster.client.java.controller.mongo.geometry;

import org.evomaster.client.java.controller.mongo.MongoGeometryRegressionCases;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeout;

class GeoJsonGeometryPerformanceTest {

    @Test
    void testDistanceBetweenDetailedDisjointLinesFinishesWithinSearchBudget() {
        // Initialize the distance calculation before measuring the larger fixture.
        assertEquals(1.0, GeoJsonGeometryIntersection.distance(line(16, 0), line(16, 1)));

        GeoJsonLineString first = line(2000, 0);
        GeoJsonLineString second = line(2000, 1);

        // MongoDB accepts these fixtures: the parallel lines do not intersect, while
        // each line intersects itself. The shared live-Mongo oracle checks that too.
        // An array-backed traversal takes tens of milliseconds locally; two seconds
        // leaves substantial headroom. Indexed LinkedList traversal adds a cubic
        // cost and takes several seconds for this otherwise modest geometry.
        // Use a synchronous timeout so a failure never leaves a worker running.
        double distance = assertTimeout(Duration.ofSeconds(2),
                () -> GeoJsonGeometryIntersection.distance(first, second));

        assertEquals(1.0, distance);
    }

    private static GeoJsonLineString line(int size, double latitude) {
        return GeoJsonUtils.toGeoJsonLineString(
                MongoGeometryRegressionCases.detailedLine(size, latitude));
    }
}
