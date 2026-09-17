package org.evomaster.client.java.controller.mongo.geometry;

public class GeoJsonPoint {

    private final double longitude;
    private final double latitude;

    /**
     * Constructs a GeoJsonPoint object with specified longitude and latitude.
     *
     * @param longitude the longitude of the point, must be between -180 and 180
     * @param latitude the latitude of the point, must be between -90 and 90
     * @throws IllegalArgumentException if the longitude is outside the range -180 to 180
     * @throws IllegalArgumentException if the latitude is outside the range -90 to 90
     */
    public GeoJsonPoint(double longitude, double latitude) {
        // Validate the longitude and latitude values
        if (longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180. But is is " + longitude);
        }
        if (latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90. But is is " + latitude);
        }
        this.longitude = longitude;
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getLatitude() {
        return latitude;
    }
}
