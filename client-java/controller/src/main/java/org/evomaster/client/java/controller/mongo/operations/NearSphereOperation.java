package org.evomaster.client.java.controller.mongo.operations;

/**
 * Represent $nearSphere operation.
 * Specifies a point for which a geospatial query returns the documents from nearest to farthest.
 */
public class NearSphereOperation extends AbstractProximityOperation {

    /**
     * Constructor for NearSphereOperation.
     * maxDistance and minDistance are optional, so they can be null.
     *
     * @param fieldName the name of the field for the proximity operation
     * @param longitude the longitude of the point for the proximity operation
     * @param latitude the latitude of the point for the proximity operation
     * @param maxDistance the maximum distance for the proximity operation (null means no maximum distance constraint)
     * @param minDistance the minimum distance for the proximity operation (null means no minimum distance constraint)
     */
    public NearSphereOperation(String fieldName,
                               double longitude,
                               double latitude,
                               Double maxDistance,
                               Double minDistance) {
        super(fieldName,
                longitude,
                latitude,
                maxDistance,
                minDistance);
    }
}
