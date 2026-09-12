package org.evomaster.client.java.controller.mongo.geometry;

public abstract class GeoJsonObject {

    private final String type;

    protected GeoJsonObject(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
