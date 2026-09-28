package org.evomaster.client.java.controller.mongo.selectors;

import org.evomaster.client.java.controller.mongo.geometry.GeoJsonLineRing;
import org.evomaster.client.java.controller.mongo.geometry.GeoJsonPoint;
import org.evomaster.client.java.controller.mongo.geometry.GeoJsonPolygon;
import org.evomaster.client.java.controller.mongo.geometry.GeoJsonUtils;
import org.evomaster.client.java.controller.mongo.operations.GeoWithinOperation;
import org.evomaster.client.java.controller.mongo.operations.QueryOperation;
import org.evomaster.client.java.utils.SimpleLogger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.evomaster.client.java.controller.mongo.utils.BsonHelper.*;

/**
 * Parses {@code $geoWithin} queries, either with a GeoJSON {@code $geometry}, or with a legacy
 * shape ({@code $box}, {@code $polygon}, {@code $center}, {@code $centerSphere}) converted into
 * an equivalent {@link GeoJsonPolygon}.
 */
public class GeoWithinSelector extends SingleConditionQuerySelector {

    private static final String GEOMETRY_OPERATOR = "$geometry";
    private static final String BOX_OPERATOR = "$box";
    private static final String POLYGON_OPERATOR = "$polygon";
    private static final String CENTER_OPERATOR = "$center";
    private static final String CENTER_SPHERE_OPERATOR = "$centerSphere";

    /** Number of sides used to approximate a $center/$centerSphere circle as a polygon. */
    private static final int CIRCLE_APPROXIMATION_SIDES = 32;

    @Override
    protected String operator() {
        return "$geoWithin";
    }

    @Override
    protected QueryOperation parseValue(String fieldName, Object value) {
        if (value == null || !isBsonDocument(value)) {
            return null;
        }
        Set<String> keys = documentKeys(value);
        if (keys == null || keys.size() != 1) {
            return null;
        }
        String shapeOperator = keys.iterator().next();
        Object shapeValue = getValue(value, shapeOperator);

        if (GEOMETRY_OPERATOR.equals(shapeOperator)) {
            try {
                return new GeoWithinOperation(fieldName, GeoJsonUtils.toGeoJsonGeometry(shapeValue));
            } catch (IllegalArgumentException e) {
                return null;
            }
        }

        final GeoJsonPolygon polygon;
        switch (shapeOperator) {
            case BOX_OPERATOR:
                polygon = parseBox(shapeValue);
                break;
            case POLYGON_OPERATOR:
                polygon = parseLegacyPolygon(shapeValue);
                break;
            case CENTER_OPERATOR:
                polygon = parseCenter(shapeValue, false);
                break;
            case CENTER_SPHERE_OPERATOR:
                polygon = parseCenter(shapeValue, true);
                break;
            default:
                return null;
        }
        if (polygon == null) {
            return null;
        }
        return new GeoWithinOperation(fieldName, polygon);
    }

    /** {@code $box: [[x1, y1], [x2, y2]]}: an axis-aligned rectangle. */
    private GeoJsonPolygon parseBox(Object value) {
        List<double[]> corners = toCoordinatePairs(value, 2);
        if (corners == null) {
            return null;
        }
        double x1 = corners.get(0)[0], y1 = corners.get(0)[1];
        double x2 = corners.get(1)[0], y2 = corners.get(1)[1];
        return toPolygon(Arrays.asList(
                new double[]{x1, y1}, new double[]{x2, y1},
                new double[]{x2, y2}, new double[]{x1, y2}, new double[]{x1, y1}
        ));
    }

    /** {@code $polygon: [[x1, y1], [x2, y2], ...]}: a polygon that MongoDB closes implicitly. */
    private GeoJsonPolygon parseLegacyPolygon(Object value) {
        List<double[]> vertices = toCoordinatePairs(value, null);
        if (vertices == null || vertices.size() < 3) {
            return null;
        }
        List<double[]> ring = new ArrayList<>(vertices);
        double[] first = ring.get(0);
        double[] last = ring.get(ring.size() - 1);
        if (first[0] != last[0] || first[1] != last[1]) {
            ring.add(first);
        }
        return toPolygon(ring);
    }

    /**
     * {@code $center: [[x, y], radius]} (planar) or {@code $centerSphere: [[x, y], radius]}
     * (spherical, radius in radians): approximates the circle as a regular polygon, since
     * {@link GeoJsonPolygon} cannot represent a true circle.
     *
     * TODO this is only an approximation (a regular polygon inscribed in the circle), not the
     * exact circle MongoDB evaluates against, so containment/intersection near the circle's
     * boundary can disagree with the real result. For $centerSphere, the radius (in radians) is
     * converted to a coordinate-space delta with a flat approximation
     * ({@code Math.toDegrees(radius)}) that ignores actual spherical geometry (e.g. longitude
     * compression away from the equator); it becomes increasingly inaccurate for larger radii or
     * centers close to the poles. A precise implementation would sample points along the actual
     * (small or great) circle on the sphere instead.
     */
    private GeoJsonPolygon parseCenter(Object value, boolean spherical) {
        if (!(value instanceof List<?>) || ((List<?>) value).size() != 2) {
            return null;
        }
        List<?> parts = (List<?>) value;
        Object centerValue = parts.get(0);
        Object radiusValue = parts.get(1);
        if (!(centerValue instanceof List<?>) || ((List<?>) centerValue).size() != 2
                || !(radiusValue instanceof Number)) {
            return null;
        }
        List<?> center = (List<?>) centerValue;
        if (!(center.get(0) instanceof Number) || !(center.get(1) instanceof Number)) {
            return null;
        }
        double centerX = ((Number) center.get(0)).doubleValue();
        double centerY = ((Number) center.get(1)).doubleValue();
        double radius = ((Number) radiusValue).doubleValue();
        if (!Double.isFinite(centerX) || !Double.isFinite(centerY) || !Double.isFinite(radius) || radius <= 0) {
            return null;
        }
        double radiusInCoordinateUnits = spherical ? Math.toDegrees(radius) : radius;

        List<double[]> ring = new ArrayList<>();
        double[] firstPoint = null;
        for (int i = 0; i < CIRCLE_APPROXIMATION_SIDES; i++) {
            double angle = 2 * Math.PI * i / CIRCLE_APPROXIMATION_SIDES;
            double[] point = new double[]{
                    centerX + radiusInCoordinateUnits * Math.cos(angle),
                    centerY + radiusInCoordinateUnits * Math.sin(angle)};
            if (i == 0) {
                firstPoint = point;
            }
            ring.add(point);
        }
        // Close the ring with the exact first point rather than recomputing cos/sin(2*PI),
        // which is not guaranteed to be bit-identical to cos/sin(0).
        ring.add(firstPoint);
        return toPolygon(ring);
    }

    /**
     * Converts a closed legacy ring (a list of [x, y] pairs, first equal to last) into a
     * {@link GeoJsonPolygon}.
     *
     * TODO legacy shapes are not required to use real longitude/latitude values: MongoDB's legacy
     * coordinate system allows arbitrary planar coordinates, with no range restriction. Building
     * on {@link GeoJsonPoint} means every vertex is validated against the GeoJSON longitude
     * ([-180, 180]) and latitude ([-90, 90]) ranges, so a legacy shape using coordinates outside
     * those ranges cannot be represented this way. Properly supporting arbitrary legacy
     * coordinates would need a separate, unvalidated planar-geometry representation instead of
     * reusing the GeoJSON classes. Until then, such shapes are rejected: a unique warning is
     * logged and parsing returns null instead of throwing.
     */
    private GeoJsonPolygon toPolygon(List<double[]> ring) {
        try {
            List<GeoJsonPoint> points = new ArrayList<>();
            for (double[] position : ring) {
                points.add(new GeoJsonPoint(position[0], position[1]));
            }
            GeoJsonLineRing exteriorRing = new GeoJsonLineRing(points);
            return new GeoJsonPolygon(exteriorRing, Collections.emptySet());
        } catch (IllegalArgumentException e) {
            SimpleLogger.uniqueWarn("Legacy $geoWithin shape uses coordinates that cannot be " +
                    "converted to a GeoJSON polygon: " + e.getMessage());
            return null;
        }
    }

    private List<double[]> toCoordinatePairs(Object value, Integer expectedSize) {
        if (!(value instanceof List<?>)) {
            return null;
        }
        List<?> list = (List<?>) value;
        if (expectedSize != null && list.size() != expectedSize) {
            return null;
        }
        List<double[]> result = new ArrayList<>();
        for (Object position : list) {
            if (!(position instanceof List<?>) || ((List<?>) position).size() != 2) {
                return null;
            }
            List<?> pair = (List<?>) position;
            if (!(pair.get(0) instanceof Number) || !(pair.get(1) instanceof Number)) {
                return null;
            }
            double x = ((Number) pair.get(0)).doubleValue();
            double y = ((Number) pair.get(1)).doubleValue();
            if (!Double.isFinite(x) || !Double.isFinite(y)) {
                return null;
            }
            result.add(new double[]{x, y});
        }
        return result;
    }
}
