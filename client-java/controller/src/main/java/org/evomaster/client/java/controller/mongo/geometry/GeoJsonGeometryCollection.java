package org.evomaster.client.java.controller.mongo.geometry;

import java.util.Collections;
import java.util.List;

public class GeoJsonGeometryCollection extends GeoJsonGeometry {

    public static final String GEOMETRY_COLLECTION_TYPE = "GeometryCollection";

    private final List<GeoJsonGeometry> geometries;

    public GeoJsonGeometryCollection(List<GeoJsonGeometry> geometries) {
        super(GEOMETRY_COLLECTION_TYPE);
        this.geometries = Collections.unmodifiableList(geometries);
    }

    public List<GeoJsonGeometry> getGeometries() {
        return geometries;
    }

    @Override
    public boolean hasArea() {
        return geometries.stream().anyMatch(GeoJsonGeometry::hasArea);
    }
}
