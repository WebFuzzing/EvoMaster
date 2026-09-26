package org.evomaster.client.java.controller.mongo.operations;

import org.evomaster.client.java.controller.mongo.geometry.GeoJsonGeometry;

import java.util.Objects;

/** A {@code $geoWithin} query with a typed GeoJSON geometry. */
public class GeoWithinOperation extends QueryOperationWithField {

    private final GeoJsonGeometry geometry;

    public GeoWithinOperation(String fieldName, GeoJsonGeometry geometry) {
        super(fieldName);
        Objects.requireNonNull(geometry);
        if (!geometry.hasArea()) {
            throw new IllegalArgumentException(
                    "A $geoWithin geometry must enclose an area, but " + geometry.getType() + " does not.");
        }
        this.geometry = geometry;
    }

    public GeoJsonGeometry getGeometry() {
        return geometry;
    }
}
