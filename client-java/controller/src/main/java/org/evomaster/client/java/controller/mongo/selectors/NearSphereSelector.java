package org.evomaster.client.java.controller.mongo.selectors;

import org.evomaster.client.java.controller.mongo.operations.NearSphereOperation;
import org.evomaster.client.java.controller.mongo.operations.QueryOperation;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import static org.evomaster.client.java.controller.mongo.utils.BsonHelper.*;

/**
 * { field: { $nearSphere: [ x, y ], $maxDistance: value, $minDistance: value } }
 * or
 * { field: { $nearSphere: {$geometry: {type: "Point", coordinates: [ longitude, latitude ]}, $maxDistance: value, $minDistance: value}}
 */
public class NearSphereSelector extends QuerySelector {

    public static final int EARTH_RADIUS_IN_METERS = 6371000;
    private static final String MIN_DISTANCE_OPERATOR = "$minDistance";
    private static final String MAX_DISTANCE_OPERATOR = "$maxDistance";
    private static final String GEOMETRY_OPERATOR = "$geometry";
    private static final String NEAR_SPHERE_OPERATOR = "$nearSphere";
    private static final String COORDINATES_FIELD_NAME = "coordinates";
    private static final String X_FIELD_NAME = "x";
    private static final String Y_FIELD_NAME = "y";

    @Override
    public QueryOperation getOperation(Object query) {
        String fieldName = extractFieldName(query);
        if (fieldName == null) {
            return null;
        }
        Object innerDoc = getValue(query, fieldName);

        if (!isBsonDocument(innerDoc) || !hasTheExpectedOperator(query)) {
            return null;
        }

        Object point = getValue(innerDoc, operator());
        if (point == null) {
            return null;
        }

        Object geometry = isBsonDocument(point) ? getValue(point, GEOMETRY_OPERATOR) : null;
        boolean legacyCoordinates = geometry == null;

        return parseValue(fieldName, innerDoc, legacyCoordinates);
    }

    protected String extractOperator(Object query) {
        String fieldName = extractFieldName(query);
        if (fieldName == null) {
            return null;
        }
        Set<String> keys = documentKeys(getValue(query, fieldName));
        return keys == null ? null : keys.stream().findFirst().orElse(null);
    }

    @Override
    protected String operator() {
        return NEAR_SPHERE_OPERATOR;
    }

    public QueryOperation parseValue(String fieldName, Object innerDoc, boolean legacyCoordinates) {
        Object longitude;
        Object latitude;
        Object maxDistance = null;
        Object minDistance = null;

        Object point = getValue(innerDoc, operator());

        if (legacyCoordinates) {
            Object maxDistanceInRadians = getValue(innerDoc, MAX_DISTANCE_OPERATOR);
            Object minDistanceInRadians = getValue(innerDoc, MIN_DISTANCE_OPERATOR);

            if (maxDistanceInRadians instanceof Double) {
                maxDistance = radiansToMeters((double) maxDistanceInRadians);
            }

            if (minDistanceInRadians instanceof Double) {
                minDistance = radiansToMeters((double) minDistanceInRadians);
            }

            longitude = getValue(point, X_FIELD_NAME);
            latitude = getValue(point, Y_FIELD_NAME);
        } else {
            Object geometry = getValue(point, GEOMETRY_OPERATOR);
            Object coordinates = getValue(geometry, COORDINATES_FIELD_NAME);

            if (coordinates instanceof List<?> && ((List<?>) coordinates).size() == 2) {
                longitude = ((List<?>) coordinates).get(0);
                latitude = ((List<?>) coordinates).get(1);
            } else {
                return null;
            }

            maxDistance = getValue(point, MAX_DISTANCE_OPERATOR);
            minDistance = getValue(point, MIN_DISTANCE_OPERATOR);
        }

        if (longitude instanceof Double && latitude instanceof Double && (maxDistance == null || maxDistance instanceof Double) && (minDistance == null || minDistance instanceof Double)) {
            return new NearSphereOperation(fieldName, (Double) longitude, (Double) latitude, (Double) maxDistance, (Double) minDistance);
        } else {
            return null;
        }
    }

    private static double radiansToMeters(double radians) {
        return EARTH_RADIUS_IN_METERS * radians;
    }

    private String extractFieldName(Object query) {
        Objects.requireNonNull(query);
        if (!isBsonDocument(query)) {
            return null;
        } else {
            Set<String> keys = documentKeys(query);
            return keys == null ? null : keys.stream().findFirst().orElse(null);
        }
    }
}
