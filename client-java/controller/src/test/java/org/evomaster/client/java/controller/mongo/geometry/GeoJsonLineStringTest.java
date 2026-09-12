package org.evomaster.client.java.controller.mongo.geometry;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GeoJsonLineStringTest {

    private final GeoJsonPoint first = new GeoJsonPoint(10, 20);
    private final GeoJsonPoint second = new GeoJsonPoint(30, 40);
    private final GeoJsonPoint third = new GeoJsonPoint(50, 60);

    @Test
    void testTwoPointsAndGeometryType() {
        GeoJsonLineString line = new GeoJsonLineString(Arrays.asList(first, second));

        assertEquals("LineString", line.getType());
        assertEquals(Arrays.asList(first, second), line.getPoints());
    }

    @Test
    void testPreservesPointOrder() {
        GeoJsonLineString line = new GeoJsonLineString(Arrays.asList(third, first, second));

        assertEquals(Arrays.asList(third, first, second), line.getPoints());
    }

    @Test
    void testRejectsNullList() {
        assertThrows(NullPointerException.class, () -> new GeoJsonLineString(null));
    }

    @Test
    void testRejectsEmptyList() {
        assertThrows(IllegalArgumentException.class,
                () -> new GeoJsonLineString(Collections.emptyList()));
    }

    @Test
    void testRejectsSinglePoint() {
        assertThrows(IllegalArgumentException.class,
                () -> new GeoJsonLineString(Collections.singletonList(first)));
    }

    @Test
    void testRejectsNullPointsAtAnyPosition() {
        for (List<GeoJsonPoint> points : Arrays.asList(
                Arrays.asList(null, first, second),
                Arrays.asList(first, null, second),
                Arrays.asList(first, second, null))) {
            NullPointerException exception = assertThrows(NullPointerException.class,
                    () -> new GeoJsonLineString(points));
            assertEquals("Points in the LineString cannot be null.", exception.getMessage());
        }
    }

    @Test
    void testRemovesConsecutiveDuplicatesAtStartMiddleAndEnd() {
        GeoJsonLineString line = new GeoJsonLineString(
                Arrays.asList(first, first, second, second, second, third, third));

        assertEquals(Arrays.asList(first, second, third), line.getPoints());
    }

    @Test
    void testRemovesEqualCoordinatesInDifferentPointObjects() {
        GeoJsonLineString line = new GeoJsonLineString(Arrays.asList(
                first, new GeoJsonPoint(10, 20), second, new GeoJsonPoint(30, 40)));

        assertEquals(Arrays.asList(first, second), line.getPoints());
    }

    @Test
    void testRejectsListContainingOnlyDuplicateCoordinates() {
        assertThrows(IllegalArgumentException.class,
                () -> new GeoJsonLineString(Arrays.asList(first, first, first)));
        assertThrows(IllegalArgumentException.class,
                () -> new GeoJsonLineString(Arrays.asList(first, new GeoJsonPoint(10, 20))));
    }

    @Test
    void testPreservesNonConsecutiveDuplicates() {
        GeoJsonLineString line = new GeoJsonLineString(Arrays.asList(first, second, first));

        assertEquals(Arrays.asList(first, second, first), line.getPoints());
    }

    @Test
    void testPointsDifferingInOnlyOneCoordinateAreDistinct() {
        GeoJsonPoint sameLongitude = new GeoJsonPoint(10, 40);
        GeoJsonPoint sameLatitude = new GeoJsonPoint(30, 40);
        GeoJsonLineString line = new GeoJsonLineString(Arrays.asList(first, sameLongitude, sameLatitude));

        assertEquals(Arrays.asList(first, sameLongitude, sameLatitude), line.getPoints());
    }

    @Test
    void testDoesNotModifyOrRetainInputList() {
        List<GeoJsonPoint> points = new ArrayList<>(Arrays.asList(first, first, second));
        GeoJsonLineString line = new GeoJsonLineString(points);

        assertEquals(Arrays.asList(first, first, second), points);
        points.clear();
        assertEquals(Arrays.asList(first, second), line.getPoints());
    }
}
