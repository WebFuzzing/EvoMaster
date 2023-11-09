package org.evomaster.client.java.controller.mongo.selectors;

import org.evomaster.client.java.controller.mongo.operations.NearSphereOperation;
import org.evomaster.client.java.controller.mongo.operations.QueryOperation;

import java.util.List;
import java.util.Set;

import static org.evomaster.client.java.controller.mongo.utils.BsonHelper.*;

/**
 * { field: { $nearSphere: [ x, y ], $maxDistance: value, $minDistance: value } }
 * or
 * { field: { $nearSphere: {$geometry: {type: "Point", coordinates: [ longitude, latitude ]}, $maxDistance: value, $minDistance: value}}
 */
public class NearSphereSelector extends QuerySelector {

    public static final int EARTH_RADIUS_IN_METERS = 6371000;

    @Override
    public QueryOperation getOperation(Object query) {
        String fieldName = extractFieldName(query);
        Object innerDoc = getValue(query, fieldName);

        if (!isDocument(innerDoc) || !hasTheExpectedOperator(query)) return null;

        Object point = getValue(innerDoc, operator());
        Object geometry = getValue(point, "$geometry");
        boolean legacyCoordinates = geometry == null;

        return parseValue(fieldName, innerDoc, legacyCoordinates);
    }

    protected String extractOperator(Object query) {
        String fieldName = extractFieldName(query);
        Set<String> keys = documentKeys(getValue(query, fieldName));
        return keys.stream().findFirst().orElse(null);
    }

    @Override
    protected String operator() {
        return "$nearSphere";
    }

    public QueryOperation parseValue(String fieldName, Object innerDoc, boolean legacyCoordinates) {
        Object longitude;
        Object latitude;
        Object maxDistance = null;
        Object minDistance = null;

        Object point = getValue(innerDoc, operator());

        if (legacyCoordinates) {
            Object maxDistanceInRadians = getValue(innerDoc, "$maxDistance");
            Object minDistanceInRadians = getValue(innerDoc, "$minDistance");

            if (maxDistanceInRadians instanceof Double) {
                maxDistance = radiansToMeters((double) maxDistanceInRadians);
            }

            if (minDistanceInRadians instanceof Double) {
                minDistance = radiansToMeters((double) minDistanceInRadians);
            }

            longitude = getValue(point, "x");
            latitude = getValue(point, "y");
        } else {
            Object geometry = getValue(point, "$geometry");
            Object coordinates = getValue(geometry, "coordinates");

            if (coordinates instanceof List<?> && ((List<?>) coordinates).size() == 2) {
                longitude = ((List<?>) coordinates).get(0);
                latitude = ((List<?>) coordinates).get(1);
            } else {
                return null;
            }

            maxDistance = getValue(point, "$maxDistance");
            minDistance = getValue(point, "$minDistance");
        }

        if (longitude instanceof Double && latitude instanceof Double && (maxDistance == null || maxDistance instanceof Double) && (minDistance == null || minDistance instanceof Double)) {
            return new NearSphereOperation(fieldName, (Double) longitude, (Double) latitude, (Double) maxDistance, (Double) minDistance);
        }
        return null;
    }

    private static double radiansToMeters(double radians) {
        return EARTH_RADIUS_IN_METERS * radians;
    }

    private String extractFieldName(Object query) {
        Set<String> keys = documentKeys(query);
        return keys.stream().findFirst().orElse(null);
    }
}