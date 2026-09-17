package org.evomaster.client.java.controller.mongo.operations;

/**
 * Represent $near operation.
 * Specifies a point for which a geospatial query returns the documents from nearest to farthest.
 */
public class NearOperation extends AbstractProximityOperation {

    private final boolean legacyCoordinates;

    public NearOperation(String fieldName,
                         double longitude,
                         double latitude,
                         Double maxDistance,
                         Double minDistance) {
        this(fieldName, longitude, latitude, maxDistance, minDistance, false);
    }

    public NearOperation(String fieldName,
                         double longitude,
                         double latitude,
                         Double maxDistance,
                         Double minDistance,
                         boolean legacyCoordinates) {
        super(fieldName, longitude, latitude, maxDistance, minDistance);
        this.legacyCoordinates = legacyCoordinates;
    }

    public boolean hasLegacyCoordinates() {
        return legacyCoordinates;
    }

}
