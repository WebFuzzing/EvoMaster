package org.evomaster.client.java.controller.mongo.operations;

/**
 * Represent $nearSphere operation.
 * Specifies a point for which a geospatial query returns the documents from nearest to farthest.
 */
public class NearSphereOperation extends QueryOperation {
    private final String fieldName;
    private final Double longitude;
    private final Double latitude;
    private final Double maxDistance;
    private final Double minDistance;


    public NearSphereOperation(String fieldName, Double longitude, Double latitude, Double maxDistance, Double minDistance) {
        this.fieldName = fieldName;
        this.longitude = longitude;
        this.latitude = latitude;
        this.maxDistance = maxDistance;
        this.minDistance = minDistance;
    }

    public String getFieldName() {
        return fieldName;
    }

    public Double getLongitude() {
        return longitude;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getMaxDistance() {
        return maxDistance;
    }

    public Double getMinDistance() {
        return minDistance;
    }
}