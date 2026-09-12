package org.evomaster.client.java.controller.mongo.geometry;

import org.evomaster.client.java.controller.mongo.utils.BsonHelper;

import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

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


    /**
     * Converts a GeoJSON LineString document into typed points.
     * Coordinates must be finite longitude/latitude pairs. Malformed documents,
     * unsupported CRS declarations, and lines with fewer than two distinct
     * consecutive positions cause an {@link IllegalArgumentException}.
     */
    public static GeoJsonLineString toGeoJsonLineString(Object document) {
        if (document == null || !BsonHelper.isBsonDocument(document)
                || !GeoJsonLineString.LINE_STRING_TYPE.equals(BsonHelper.getValue(document, TYPE))
                || BsonHelper.documentContainsField(document, "crs")) {
            throw new IllegalArgumentException("The provided document is not a supported GeoJSON LineString.");
        }
        return toLineString(BsonHelper.getValue(document, COORDINATES));
    }

    /**
     * Converts a GeoJSON MultiLineString document into a typed {@link GeoJsonMultiLineString}.
     * The coordinates must be a non-empty list of LineString coordinate arrays, each one
     * validated the same way as a standalone LineString's coordinates.
     */
    public static GeoJsonMultiLineString toGeoJsonMultiLineString(Object document) {
        if (document == null || !BsonHelper.isBsonDocument(document)
                || !GeoJsonMultiLineString.MULTI_LINE_STRING_TYPE.equals(BsonHelper.getValue(document, TYPE))
                || BsonHelper.documentContainsField(document, "crs")) {
            throw new IllegalArgumentException("The provided document is not a supported GeoJSON MultiLineString.");
        }
        Object coordinates = BsonHelper.getValue(document, COORDINATES);
        if (!(coordinates instanceof List<?>) || ((List<?>) coordinates).isEmpty()) {
            throw new IllegalArgumentException("MultiLineString coordinates must be a non-empty list of line coordinates.");
        }
        List<GeoJsonLineString> lineStrings = new ArrayList<>();
        for (Object lineCoordinates : (List<?>) coordinates) {
            lineStrings.add(toLineString(lineCoordinates));
        }
        return new GeoJsonMultiLineString(lineStrings);
    }

    private static GeoJsonLineString toLineString(Object coordinates) {
        if (!(coordinates instanceof List<?>)) {
            throw new IllegalArgumentException("LineString coordinates must be a list of positions.");
        }
        return new GeoJsonLineString(toPoints((List<?>) coordinates));
    }

    /**
     * Converts a GeoJSON MultiPoint document into a typed {@link GeoJsonMultiPoint}.
     * Coordinates must be finite longitude/latitude pairs. Malformed documents and
     * unsupported CRS declarations cause an {@link IllegalArgumentException}.
     */
    public static GeoJsonMultiPoint toGeoJsonMultiPoint(Object document) {
        if (document == null || !BsonHelper.isBsonDocument(document)
                || !GeoJsonMultiPoint.MULTI_POINT_TYPE.equals(BsonHelper.getValue(document, TYPE))
                || BsonHelper.documentContainsField(document, "crs")) {
            throw new IllegalArgumentException("The provided document is not a supported GeoJSON MultiPoint.");
        }
        Object coordinates = BsonHelper.getValue(document, COORDINATES);
        if (!(coordinates instanceof List<?>)) {
            throw new IllegalArgumentException("MultiPoint coordinates must be a list of positions.");
        }
        return new GeoJsonMultiPoint(toPoints((List<?>) coordinates));
    }

    /**
     * Converts a GeoJSON Polygon document into a typed {@link GeoJsonPolygon}.
     * The first ring in the coordinates is the exterior ring, and any subsequent
     * rings are holes. Each ring must be a closed linear ring, as validated by
     * {@link GeoJsonLineRing}. Malformed documents, unsupported CRS declarations,
     * and polygons without an exterior ring cause an {@link IllegalArgumentException}.
     */
    public static GeoJsonPolygon toGeoJsonPolygon(Object document) {
        if (document == null || !BsonHelper.isBsonDocument(document)
                || !GeoJsonPolygon.POLYGON_TYPE.equals(BsonHelper.getValue(document, TYPE))
                || BsonHelper.documentContainsField(document, "crs")) {
            throw new IllegalArgumentException("The provided document is not a supported GeoJSON Polygon.");
        }
        return toPolygon(BsonHelper.getValue(document, COORDINATES));
    }

    /**
     * Converts a GeoJSON MultiPolygon document into a typed {@link GeoJsonMultiPolygon}.
     * The coordinates must be a non-empty list of Polygon coordinate arrays, each one
     * validated the same way as a standalone Polygon's coordinates.
     */
    public static GeoJsonMultiPolygon toGeoJsonMultiPolygon(Object document) {
        if (document == null || !BsonHelper.isBsonDocument(document)
                || !GeoJsonMultiPolygon.MULTI_POLYGON_TYPE.equals(BsonHelper.getValue(document, TYPE))
                || BsonHelper.documentContainsField(document, "crs")) {
            throw new IllegalArgumentException("The provided document is not a supported GeoJSON MultiPolygon.");
        }
        Object coordinates = BsonHelper.getValue(document, COORDINATES);
        if (!(coordinates instanceof List<?>) || ((List<?>) coordinates).isEmpty()) {
            throw new IllegalArgumentException("MultiPolygon coordinates must be a non-empty list of polygon coordinates.");
        }
        List<GeoJsonPolygon> polygons = new ArrayList<>();
        for (Object polygonCoordinates : (List<?>) coordinates) {
            polygons.add(toPolygon(polygonCoordinates));
        }
        return new GeoJsonMultiPolygon(polygons);
    }

    private static GeoJsonPolygon toPolygon(Object coordinates) {
        if (!(coordinates instanceof List<?>) || ((List<?>) coordinates).isEmpty()) {
            throw new IllegalArgumentException("Polygon coordinates must be a non-empty list of linear rings.");
        }
        List<?> rings = (List<?>) coordinates;
        GeoJsonLineRing exteriorRing = toGeoJsonLineRing(rings.get(0));
        Set<GeoJsonLineRing> interiorRings = new LinkedHashSet<>();
        for (int i = 1; i < rings.size(); i++) {
            interiorRings.add(toGeoJsonLineRing(rings.get(i)));
        }
        return new GeoJsonPolygon(exteriorRing, interiorRings);
    }

    private static GeoJsonLineRing toGeoJsonLineRing(Object ringCoordinates) {
        if (!(ringCoordinates instanceof List<?>)) {
            throw new IllegalArgumentException("A linear ring must be a list of positions.");
        }
        return new GeoJsonLineRing(toPoints((List<?>) ringCoordinates));
    }

    private static List<GeoJsonPoint> toPoints(List<?> positions) {
        List<GeoJsonPoint> points = new ArrayList<>();
        for (Object position : positions) {
            if (!(position instanceof List<?>) || ((List<?>) position).size() != 2) {
                throw new IllegalArgumentException("A position must contain longitude and latitude.");
            }
            List<?> pair = (List<?>) position;
            if (!(pair.get(0) instanceof Number) || !(pair.get(1) instanceof Number)) {
                throw new IllegalArgumentException("Coordinates must be numbers.");
            }
            double longitude = ((Number) pair.get(0)).doubleValue();
            double latitude = ((Number) pair.get(1)).doubleValue();
            if (!Double.isFinite(longitude) || !Double.isFinite(latitude)) {
                throw new IllegalArgumentException("Coordinates must be finite.");
            }
            points.add(new GeoJsonPoint(longitude, latitude));
        }
        return points;
    }

}
