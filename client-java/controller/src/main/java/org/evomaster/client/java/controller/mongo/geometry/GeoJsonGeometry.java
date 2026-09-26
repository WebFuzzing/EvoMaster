package org.evomaster.client.java.controller.mongo.geometry;

public abstract class GeoJsonGeometry {

    private final String type;

    protected GeoJsonGeometry(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    /**
     * Whether this geometry encloses an area (as opposed to being a point or a line).
     * True for {@link GeoJsonPolygon} and {@link GeoJsonMultiPolygon}; for
     * {@link GeoJsonGeometryCollection}, true when any of its members has an area.
     */
    public abstract boolean hasArea();
}
