package org.evomaster.client.java.controller.mongo.geometry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GeoJsonPointTest {
    @Test
    void testValidCoordinates() {
        // Test valid coordinates
        GeoJsonPoint point = new GeoJsonPoint(45.0, 30.0);
        assertEquals(45.0, point.getLongitude());
        assertEquals(30.0, point.getLatitude());
    }

    @Test
    void testInvalidLongitude() {
        // Test invalid longitude
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            new GeoJsonPoint(-200.0, 30.0);
        });
        assertTrue(exception.getMessage().contains("Longitude must be between -180 and 180"));
    }

    @Test
    void testInvalidLatitude() {
        // Test invalid latitude
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            new GeoJsonPoint(45.0, 100.0);
        });
        assertTrue(exception.getMessage().contains("Latitude must be between -90 and 90"));
    }


}
