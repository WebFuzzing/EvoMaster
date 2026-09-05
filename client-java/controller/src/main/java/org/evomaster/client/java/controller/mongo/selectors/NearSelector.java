package org.evomaster.client.java.controller.mongo.selectors;

import org.evomaster.client.java.controller.mongo.operations.AbstractProximityOperation;
import org.evomaster.client.java.controller.mongo.operations.NearOperation;

/**
 * Parses {@code $near} queries using either legacy coordinate pairs or GeoJSON points.
 */
public class NearSelector extends NearSphereSelector {

    private static final String NEAR_OPERATOR = "$near";

    @Override
    protected String operator() {
        return NEAR_OPERATOR;
    }

    @Override
    protected double convertLegacyDistance(double distance) {
        return distance;
    }

    @Override
    protected AbstractProximityOperation createOperation(String fieldName,
                                                         double longitude,
                                                         double latitude,
                                                         Double maxDistance,
                                                         Double minDistance,
                                                         boolean legacyCoordinates) {
        return new NearOperation(fieldName, longitude, latitude, maxDistance, minDistance, legacyCoordinates);
    }
}
