package org.evomaster.client.java.controller.mongo.geometry;

import java.util.Collections;
import java.util.List;

public class GeoJsonMultiPolygon extends GeoJsonGeometry {

    public static final String MULTI_POLYGON_TYPE = "MultiPolygon";

    private final List<GeoJsonPolygon> polygons;

    public GeoJsonMultiPolygon(List<GeoJsonPolygon> polygons) {
        super(MULTI_POLYGON_TYPE);
        this.polygons = Collections.unmodifiableList(polygons);
    }

    public List<GeoJsonPolygon> getPolygons() {
        return polygons;
    }

    @Override
    public boolean hasArea() {
        return true;
    }

    public String toString() {
        return "GeoJsonMultiPolygon{" +
                "polygons=" + polygons +
                '}';
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof GeoJsonMultiPolygon)) {
            return false;
        }
        GeoJsonMultiPolygon that = (GeoJsonMultiPolygon) obj;
        return polygons.equals(that.polygons);
    }

    public int hashCode() {
        return polygons.hashCode();
    }
}
