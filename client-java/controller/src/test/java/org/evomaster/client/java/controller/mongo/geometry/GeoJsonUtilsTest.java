package org.evomaster.client.java.controller.mongo.geometry;

import org.bson.Document;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class GeoJsonUtilsTest {

    @Test
    void testIsGeoJsonPointNullDocument() {
        assertThrows(NullPointerException.class, () -> GeoJsonUtils.isGeoJsonPoint(null));
    }

    @Test
    void testIsGeoJsonPointNotBsonDocument() {
        assertThrows(IllegalArgumentException.class, () -> GeoJsonUtils.isGeoJsonPoint(new Object()));
        assertThrows(IllegalArgumentException.class, () -> GeoJsonUtils.isGeoJsonPoint("Not a BSON Document"));
        assertThrows(IllegalArgumentException.class, () -> GeoJsonUtils.isGeoJsonPoint(123));
    }

    @Test
    void testIsGeoJsonPointValid() {
        Document validDoc = new Document("type", "Point")
                .append("coordinates", Arrays.asList(45.0, 30.0));
        assertTrue(GeoJsonUtils.isGeoJsonPoint(validDoc));

        Document intCoordinatesDoc = new Document("type", "Point")
                .append("coordinates", Arrays.asList(45, 30));
        assertTrue(GeoJsonUtils.isGeoJsonPoint(intCoordinatesDoc));

        Document doubleCoordinatesDoc = new Document("type", "Point")
                .append("coordinates", Arrays.asList(-122.4194, 37.7749));
        assertTrue(GeoJsonUtils.isGeoJsonPoint(doubleCoordinatesDoc));
    }

    @Test
    void testIsGeoJsonPointBoundaryCoordinates() {
        // Minimum valid coordinates [-180, -90]
        Document minCoordsDoc = new Document("type", "Point")
                .append("coordinates", Arrays.asList(-180.0, -90.0));
        assertTrue(GeoJsonUtils.isGeoJsonPoint(minCoordsDoc));

        // Maximum valid coordinates [180, 90]
        Document maxCoordsDoc = new Document("type", "Point")
                .append("coordinates", Arrays.asList(180.0, 90.0));
        assertTrue(GeoJsonUtils.isGeoJsonPoint(maxCoordsDoc));

        // Origin [0, 0]
        Document zeroCoordsDoc = new Document("type", "Point")
                .append("coordinates", Arrays.asList(0.0, 0.0));
        assertTrue(GeoJsonUtils.isGeoJsonPoint(zeroCoordsDoc));

        // Mixed boundaries [-180, 90] and [180, -90]
        Document mixed1 = new Document("type", "Point")
                .append("coordinates", Arrays.asList(-180, 90));
        assertTrue(GeoJsonUtils.isGeoJsonPoint(mixed1));

        Document mixed2 = new Document("type", "Point")
                .append("coordinates", Arrays.asList(180, -90));
        assertTrue(GeoJsonUtils.isGeoJsonPoint(mixed2));
    }

    @Test
    void testIsGeoJsonPointInvalidType() {
        // Missing type
        Document missingTypeDoc = new Document("coordinates", Arrays.asList(45.0, 30.0));
        assertFalse(GeoJsonUtils.isGeoJsonPoint(missingTypeDoc));

        // Null type
        Document nullTypeDoc = new Document("type", null)
                .append("coordinates", Arrays.asList(45.0, 30.0));
        assertFalse(GeoJsonUtils.isGeoJsonPoint(nullTypeDoc));

        // Other GeoJSON types
        Document polygonDoc = new Document("type", "Polygon")
                .append("coordinates", Arrays.asList(45.0, 30.0));
        assertFalse(GeoJsonUtils.isGeoJsonPoint(polygonDoc));

        Document lineStringDoc = new Document("type", "LineString")
                .append("coordinates", Arrays.asList(45.0, 30.0));
        assertFalse(GeoJsonUtils.isGeoJsonPoint(lineStringDoc));

        // Case-sensitive check ("point" instead of "Point")
        Document lowercasePointDoc = new Document("type", "point")
                .append("coordinates", Arrays.asList(45.0, 30.0));
        assertFalse(GeoJsonUtils.isGeoJsonPoint(lowercasePointDoc));

        // Non-string type
        Document nonStringTypeDoc = new Document("type", 123)
                .append("coordinates", Arrays.asList(45.0, 30.0));
        assertFalse(GeoJsonUtils.isGeoJsonPoint(nonStringTypeDoc));
    }

    @Test
    void testIsGeoJsonPointInvalidCoordinatesStructure() {
        // Missing coordinates
        Document missingCoordsDoc = new Document("type", "Point");
        assertFalse(GeoJsonUtils.isGeoJsonPoint(missingCoordsDoc));

        // Null coordinates
        Document nullCoordsDoc = new Document("type", "Point")
                .append("coordinates", null);
        assertFalse(GeoJsonUtils.isGeoJsonPoint(nullCoordsDoc));

        // Coordinates not a List
        Document stringCoordsDoc = new Document("type", "Point")
                .append("coordinates", "45.0, 30.0");
        assertFalse(GeoJsonUtils.isGeoJsonPoint(stringCoordsDoc));

        Document intCoordsDoc = new Document("type", "Point")
                .append("coordinates", 100);
        assertFalse(GeoJsonUtils.isGeoJsonPoint(intCoordsDoc));

        Document subDocCoordsDoc = new Document("type", "Point")
                .append("coordinates", new Document("lng", 45.0).append("lat", 30.0));
        assertFalse(GeoJsonUtils.isGeoJsonPoint(subDocCoordsDoc));

        // Coordinates list with size != 2
        Document emptyListDoc = new Document("type", "Point")
                .append("coordinates", Collections.emptyList());
        assertFalse(GeoJsonUtils.isGeoJsonPoint(emptyListDoc));

        Document oneElementDoc = new Document("type", "Point")
                .append("coordinates", Collections.singletonList(45.0));
        assertFalse(GeoJsonUtils.isGeoJsonPoint(oneElementDoc));

        Document threeElementsDoc = new Document("type", "Point")
                .append("coordinates", Arrays.asList(45.0, 30.0, 10.0));
        assertFalse(GeoJsonUtils.isGeoJsonPoint(threeElementsDoc));
    }

    @Test
    void testIsGeoJsonPointNonNumericCoordinates() {
        // Non-number elements
        Document stringValuesDoc = new Document("type", "Point")
                .append("coordinates", Arrays.asList("45.0", "30.0"));
        assertFalse(GeoJsonUtils.isGeoJsonPoint(stringValuesDoc));

        Document firstStringDoc = new Document("type", "Point")
                .append("coordinates", Arrays.asList("45.0", 30.0));
        assertFalse(GeoJsonUtils.isGeoJsonPoint(firstStringDoc));

        Document secondStringDoc = new Document("type", "Point")
                .append("coordinates", Arrays.asList(45.0, "30.0"));
        assertFalse(GeoJsonUtils.isGeoJsonPoint(secondStringDoc));

        // Null elements in list
        Document nullFirstDoc = new Document("type", "Point")
                .append("coordinates", Arrays.asList(null, 30.0));
        assertFalse(GeoJsonUtils.isGeoJsonPoint(nullFirstDoc));

        Document nullSecondDoc = new Document("type", "Point")
                .append("coordinates", Arrays.asList(45.0, null));
        assertFalse(GeoJsonUtils.isGeoJsonPoint(nullSecondDoc));

        Document allNullDoc = new Document("type", "Point")
                .append("coordinates", Arrays.asList(null, null));
        assertFalse(GeoJsonUtils.isGeoJsonPoint(allNullDoc));
    }

    @Test
    void testIsGeoJsonPointCoordinatesOutOfRange() {
        // Longitude < -180
        Document longitudeTooSmall = new Document("type", "Point")
                .append("coordinates", Arrays.asList(-180.1, 0.0));
        assertFalse(GeoJsonUtils.isGeoJsonPoint(longitudeTooSmall));

        // Longitude > 180
        Document longitudeTooLarge = new Document("type", "Point")
                .append("coordinates", Arrays.asList(180.1, 0.0));
        assertFalse(GeoJsonUtils.isGeoJsonPoint(longitudeTooLarge));

        // Latitude < -90
        Document latitudeTooSmall = new Document("type", "Point")
                .append("coordinates", Arrays.asList(0.0, -90.1));
        assertFalse(GeoJsonUtils.isGeoJsonPoint(latitudeTooSmall));

        // Latitude > 90
        Document latitudeTooLarge = new Document("type", "Point")
                .append("coordinates", Arrays.asList(0.0, 90.1));
        assertFalse(GeoJsonUtils.isGeoJsonPoint(latitudeTooLarge));

        // Both out of range
        Document bothOutOfRange = new Document("type", "Point")
                .append("coordinates", Arrays.asList(200.0, 100.0));
        assertFalse(GeoJsonUtils.isGeoJsonPoint(bothOutOfRange));
    }

    @Test
    void testToGeoJsonPointValid() {
        Document doc = new Document("type", "Point")
                .append("coordinates", Arrays.asList(45.5, -30.5));
        GeoJsonPoint point = GeoJsonUtils.toGeoJsonPoint(doc);

        assertNotNull(point);
        assertEquals(45.5, point.getLongitude());
        assertEquals(-30.5, point.getLatitude());

        Document intDoc = new Document("type", "Point")
                .append("coordinates", Arrays.asList(100, -45));
        GeoJsonPoint intPoint = GeoJsonUtils.toGeoJsonPoint(intDoc);

        assertNotNull(intPoint);
        assertEquals(100.0, intPoint.getLongitude());
        assertEquals(-45.0, intPoint.getLatitude());
    }

    @Test
    void testToGeoJsonPointBoundaryCoordinates() {
        Document minDoc = new Document("type", "Point")
                .append("coordinates", Arrays.asList(-180.0, -90.0));
        GeoJsonPoint minPoint = GeoJsonUtils.toGeoJsonPoint(minDoc);
        assertEquals(-180.0, minPoint.getLongitude());
        assertEquals(-90.0, minPoint.getLatitude());

        Document maxDoc = new Document("type", "Point")
                .append("coordinates", Arrays.asList(180.0, 90.0));
        GeoJsonPoint maxPoint = GeoJsonUtils.toGeoJsonPoint(maxDoc);
        assertEquals(180.0, maxPoint.getLongitude());
        assertEquals(90.0, maxPoint.getLatitude());
    }

    @Test
    void testToGeoJsonPointInvalidDocument() {
        assertThrows(NullPointerException.class, () -> GeoJsonUtils.toGeoJsonPoint(null));
        assertThrows(IllegalArgumentException.class, () -> GeoJsonUtils.toGeoJsonPoint(new Object()));

        // Document that is not a valid GeoJSON Point
        Document invalidTypeDoc = new Document("type", "Polygon")
                .append("coordinates", Arrays.asList(45.0, 30.0));
        IllegalArgumentException e1 = assertThrows(IllegalArgumentException.class,
                () -> GeoJsonUtils.toGeoJsonPoint(invalidTypeDoc));
        assertEquals("The provided document is not a valid GeoJSON Point.", e1.getMessage());

        Document outOfRangeDoc = new Document("type", "Point")
                .append("coordinates", Arrays.asList(200.0, 30.0));
        IllegalArgumentException e2 = assertThrows(IllegalArgumentException.class,
                () -> GeoJsonUtils.toGeoJsonPoint(outOfRangeDoc));
        assertEquals("The provided document is not a valid GeoJSON Point.", e2.getMessage());

        Document malformedCoordsDoc = new Document("type", "Point")
                .append("coordinates", Collections.singletonList(45.0));
        IllegalArgumentException e3 = assertThrows(IllegalArgumentException.class,
                () -> GeoJsonUtils.toGeoJsonPoint(malformedCoordsDoc));
        assertEquals("The provided document is not a valid GeoJSON Point.", e3.getMessage());
    }
}
