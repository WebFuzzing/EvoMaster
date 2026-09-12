package org.evomaster.client.java.controller.mongo.operations;

import org.evomaster.client.java.controller.mongo.geometry.GeoJsonObject;

import java.util.Objects;

/** A {@code $geoIntersects} query with a typed GeoJSON geometry. */
public class GeoIntersectsOperation extends QueryOperationWithField {

    private final GeoJsonObject geometry;

    public GeoIntersectsOperation(String fieldName, GeoJsonObject geometry) {
        super(fieldName);
        this.geometry = Objects.requireNonNull(geometry);
    }

    public GeoJsonObject getGeometry() {
        return geometry;
    }
}
