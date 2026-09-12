package org.evomaster.client.java.controller.mongo.operations;

import org.evomaster.client.java.controller.mongo.geometry.GeoJsonGeometry;

import java.util.Objects;

/** A {@code $geoIntersects} query with a typed GeoJSON geometry. */
public class GeoIntersectsOperation extends QueryOperationWithField {

    private final GeoJsonGeometry geometry;

    public GeoIntersectsOperation(String fieldName, GeoJsonGeometry geometry) {
        super(fieldName);
        this.geometry = Objects.requireNonNull(geometry);
    }

    public GeoJsonGeometry getGeometry() {
        return geometry;
    }
}
