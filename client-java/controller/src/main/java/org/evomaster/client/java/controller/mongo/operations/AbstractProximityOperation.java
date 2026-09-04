package org.evomaster.client.java.controller.mongo.operations;

/**
 * Represent an abstract proximitiy operation.
 * It could be a $near or $nearSphere operation.
 */
public abstract class AbstractProximityOperation extends QueryOperationWithField {
    private final double longitude;
    private final double latitude;
    private final Double maxDistance;
    private final Double minDistance;


    /**
     * Constructor for AbstractProximityOperation.
     * maxDistance and minDistance are optional, so they can be null.
     *
     * @param fieldName
     * @param longitude
     * @param latitude
     * @param maxDistance the maximum distance for the proximity operation
     * @param minDistance the minimum distance for the proximity operation
     */
    public AbstractProximityOperation(String fieldName,
                                      double longitude,
                                      double latitude,
                                      Double maxDistance,
                                      Double minDistance) {
        super(fieldName);
        this.longitude = longitude;
        this.latitude = latitude;
        this.maxDistance = maxDistance;
        this.minDistance = minDistance;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    /**
     * The maximum distance for the proximity operation. If null, it
     * means there is no maximum distance constraint.
     *
     * @return the maximum distance for the proximity operation
     */
    public Double getMaxDistance() {
        return maxDistance;
    }

    /**
     * The minimum distance for the proximity operation. If null, it
     * means there is no minimum distance constraint.
     *
     * @return the minimum distance for the proximity operation
     *
     */
    public Double getMinDistance() {
        return minDistance;
    }

    /**
     * Check if the proximity operation has a maximum distance constraint.
     *
     * @return true if the proximity operation has a maximum distance constraint, false otherwise
     */
    public boolean hasMaxDistance() {
        return maxDistance != null;
    }

    /**
     * Check if the proximity operation has a minimum distance constraint.
     *
     * @return true if the proximity operation has a minimum distance constraint, false otherwise
     */
    public boolean hasMinDistance() {
        return minDistance != null;
    }
}
