package org.evomaster.client.java.controller.mongo.selectors;

import org.evomaster.client.java.controller.mongo.geometry.GeoJsonUtils;
import org.evomaster.client.java.controller.mongo.operations.AbstractProximityOperation;
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

        if (!isBsonDocument(innerDoc) || !documentContainsField(innerDoc, operator())) {
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
        Number longitude;
        Number latitude;
        Double maxDistance;
        Double minDistance;

        Object point = getValue(innerDoc, operator());
        Object rawMaxDistance = getValue(legacyCoordinates ? innerDoc : point, MAX_DISTANCE_OPERATOR);
        Object rawMinDistance = getValue(legacyCoordinates ? innerDoc : point, MIN_DISTANCE_OPERATOR);

        if ((rawMaxDistance != null && !(rawMaxDistance instanceof Number))
                || (rawMinDistance != null && !(rawMinDistance instanceof Number))) {
            return null;
        }

        maxDistance = rawMaxDistance == null ? null : ((Number) rawMaxDistance).doubleValue();
        minDistance = rawMinDistance == null ? null : ((Number) rawMinDistance).doubleValue();

        if (legacyCoordinates) {
            if (point instanceof List<?> && ((List<?>) point).size() == 2) {
                Object rawLongitude = ((List<?>) point).get(0);
                Object rawLatitude = ((List<?>) point).get(1);
                if (!(rawLongitude instanceof Number) || !(rawLatitude instanceof Number)) {
                    return null;
                }
                longitude = (Number) rawLongitude;
                latitude = (Number) rawLatitude;
            } else if (isBsonDocument(point)) {
                Object rawLongitude = getValue(point, X_FIELD_NAME);
                Object rawLatitude = getValue(point, Y_FIELD_NAME);
                if (!(rawLongitude instanceof Number) || !(rawLatitude instanceof Number)) {
                    return null;
                }
                longitude = (Number) rawLongitude;
                latitude = (Number) rawLatitude;
            } else {
                return null;
            }

            maxDistance = maxDistance == null ? null : convertLegacyDistance(maxDistance);
            minDistance = minDistance == null ? null : convertLegacyDistance(minDistance);
        } else {
            Object geometry = getValue(point, GEOMETRY_OPERATOR);
            if (!isBsonDocument(geometry) || !GeoJsonUtils.isGeoJsonPoint(geometry)) {
                return null;
            }
            Object coordinates = getValue(geometry, COORDINATES_FIELD_NAME);
            longitude = (Number) ((List<?>) coordinates).get(0);
            latitude = (Number) ((List<?>) coordinates).get(1);
        }

        return createOperation(fieldName, longitude.doubleValue(), latitude.doubleValue(), maxDistance, minDistance, legacyCoordinates);
    }

    protected double convertLegacyDistance(double radians) {
        return EARTH_RADIUS_IN_METERS * radians;
    }

    protected AbstractProximityOperation createOperation(String fieldName,
                                                         double longitude,
                                                         double latitude,
                                                         Double maxDistance,
                                                         Double minDistance,
                                                         boolean legacyCoordinates) {
        return new NearSphereOperation(fieldName, longitude, latitude, maxDistance, minDistance);
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
