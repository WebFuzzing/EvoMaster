package org.evomaster.client.java.controller.mongo.geometry;

public abstract class GeoJsonGeometry {

    private final String type;

    protected GeoJsonGeometry(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
