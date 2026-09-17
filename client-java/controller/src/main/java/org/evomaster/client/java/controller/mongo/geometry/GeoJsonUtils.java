package org.evomaster.client.java.controller.mongo.geometry;

import org.evomaster.client.java.controller.mongo.utils.BsonHelper;

import java.util.List;
import java.util.Objects;

public abstract class GeoJsonUtils {

    private static final String POINT = "Point";
    private static final String COORDINATES = "coordinates";
    private static final String TYPE = "type";

    /**
     * Checks whether the given object represents a GeoJSON Point.
     * A valid GeoJSON Point must be a BSON document with the "type" field
     * set to "Point" and a "coordinates" field containing a list of exactly
     * two numerical values: longitude and latitude.
     *
     * @param document the object to validate as a GeoJSON Point. Must be a BSON document.
     * @return {@code true} if the object represents a valid GeoJSON Point, {@code false} otherwise.
     * @throws NullPointerException     if the provided document is {@code null}.
     * @throws IllegalArgumentException if the provided document is not a BSON document.
     */
    public static boolean isGeoJsonPoint(Object document) {
        Objects.requireNonNull(document);

        if (!BsonHelper.isBsonDocument(document)) {
            throw new IllegalArgumentException("argument document must be a BsonDocument");
        }

        Object typeValue = BsonHelper.getValue(document, TYPE);
        if (typeValue == null || !typeValue.equals(POINT)) {
            return false;
        }

        Object coordinatesValue = BsonHelper.getValue(document, COORDINATES);
        if (!(coordinatesValue instanceof List<?>)) {
            return false;
        }

        List<?> coordinatesList = (List<?>) coordinatesValue;
        if (coordinatesList.size() != 2) {
            return false;
        }

        Object longitude = coordinatesList.get(0);
        Object latitude = coordinatesList.get(1);
        if (!(longitude instanceof Number && latitude instanceof Number)) {
            return false;
        }

        double longitudeDoubleValue = ((Number) longitude).doubleValue();
        double latitudeDoubleValue = ((Number) latitude).doubleValue();

        if (longitudeDoubleValue >= -180 && longitudeDoubleValue <= 180 && latitudeDoubleValue >= -90 && latitudeDoubleValue <= 90) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Converts an object representing a valid GeoJSON Point into a {@code GeoJsonPoint} instance.
     * The input object must be a BSON document containing a "type" field with the value "Point"
     * and a "coordinates" field with a list of exactly two numerical values: longitude and latitude.
     *
     * @param document the BSON document to convert to a {@code GeoJsonPoint}. Must represent a valid GeoJSON Point.
     * @return a {@code GeoJsonPoint} constructed from the longitude and latitude values in the provided document.
     * @throws IllegalArgumentException if the provided object is not a valid GeoJSON Point.
     */
    public static GeoJsonPoint toGeoJsonPoint(Object document) {
        if (!isGeoJsonPoint(document)) {
            throw new IllegalArgumentException("The provided document is not a valid GeoJSON Point.");
        }

        List<?> coordinatesList = (List<?>) BsonHelper.getValue(document, COORDINATES);
        double longitude = ((Number) coordinatesList.get(0)).doubleValue();
        double latitude = ((Number) coordinatesList.get(1)).doubleValue();

        return new GeoJsonPoint(longitude, latitude);
    }


}
