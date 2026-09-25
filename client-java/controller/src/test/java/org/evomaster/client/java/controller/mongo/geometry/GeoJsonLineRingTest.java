package org.evomaster.client.java.controller.mongo.geometry;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GeoJsonLineRingTest {

    private final GeoJsonPoint first = new GeoJsonPoint(0, 0);
    private final GeoJsonPoint second = new GeoJsonPoint(10, 0);
    private final GeoJsonPoint third = new GeoJsonPoint(10, 10);
    private final GeoJsonPoint fourth = new GeoJsonPoint(0, 10);

    @Test
    void testClosedRingWithFourDistinctPoints() {
        GeoJsonPoint closing = new GeoJsonPoint(0, 0);
        List<GeoJsonPoint> points = Arrays.asList(first, second, third, fourth, closing);
        GeoJsonLineString ring = new GeoJsonLineRing(points);

        assertEquals("LineString", ring.getType());
        assertEquals(points, ring.getPoints());
        assertEquals(5, ring.getPoints().size());
    }

    @Test
    void testAllowsMoreThanFourDistinctPoints() {
        List<GeoJsonPoint> points = Arrays.asList(
                first, second, third, new GeoJsonPoint(5, 15), fourth, first);

        assertEquals(points, new GeoJsonLineRing(points).getPoints());
    }

    @Disabled("Self-intersection detection is not implemented in the current GeoJsonLineRing class.")
    @Test
    void testRejectsSelfIntersectingRing() {
        // The non-adjacent diagonal edges cross at (5, 5).
        List<GeoJsonPoint> points = Arrays.asList(first, third, second, fourth, first);

        assertThrows(IllegalArgumentException.class, () -> new GeoJsonLineRing(points));
    }

    @Test
    void testRejectsOpenRing() {
        assertThrows(IllegalArgumentException.class,
                () -> new GeoJsonLineRing(Arrays.asList(first, second, third, fourth)));
        assertThrows(IllegalArgumentException.class,
                () -> new GeoJsonLineRing(Arrays.asList(first, second, third, fourth, new GeoJsonPoint(0, 1))));
        assertThrows(IllegalArgumentException.class,
                () -> new GeoJsonLineRing(Arrays.asList(first, second, third, fourth, new GeoJsonPoint(1, 0))));
    }

    @Test
    void testRemovesConsecutiveDuplicatesBeforeValidation() {
        GeoJsonLineRing ring = new GeoJsonLineRing(Arrays.asList(
                first, new GeoJsonPoint(0, 0), second, third, third, fourth, first, first));

        assertEquals(Arrays.asList(first, second, third, fourth, first), ring.getPoints());
        assertThrows(IllegalArgumentException.class,
                () -> new GeoJsonLineRing(Arrays.asList(first, first, second, second, first)));
    }

    @Test
    void testRejectsNullOrInsufficientInput() {
        assertThrows(NullPointerException.class, () -> new GeoJsonLineRing(null));
        assertThrows(NullPointerException.class,
                () -> new GeoJsonLineRing(Arrays.asList(first, second, null, fourth, first)));
        assertThrows(IllegalArgumentException.class, () -> new GeoJsonLineRing(Collections.emptyList()));
        assertThrows(IllegalArgumentException.class,
                () -> new GeoJsonLineRing(Collections.singletonList(first)));
    }

    @Test
    void testRingCannotBeOpenedByMutatingLists() {
        List<GeoJsonPoint> points = new ArrayList<>(Arrays.asList(first, second, third, fourth, first));
        GeoJsonLineRing ring = new GeoJsonLineRing(points);
        points.clear();

        assertEquals(Arrays.asList(first, second, third, fourth, first), ring.getPoints());
        assertThrows(UnsupportedOperationException.class, () -> ring.getPoints().clear());
        assertThrows(UnsupportedOperationException.class, () -> ring.getPoints().set(4, second));
    }
}
