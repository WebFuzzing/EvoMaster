package org.evomaster.client.java.controller.mongo.geometry;

import java.util.Collections;
import java.util.List;

public class GeoJsonMultiPoint extends GeoJsonGeometry {

    public static final String MULTI_POINT_TYPE = "MultiPoint";

    private final List<GeoJsonPoint> points;

    public GeoJsonMultiPoint(List<GeoJsonPoint> points) {
        super(MULTI_POINT_TYPE);
        this.points = Collections.unmodifiableList(points);
    }

    public List<GeoJsonPoint> getPoints() {
        return points;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("GeoJsonMultiPoint{");
        sb.append("points=[");
        for (int i = 0; i < points.size(); i++) {
            sb.append(points.get(i));
            if (i < points.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append("]");
        sb.append('}');
        return sb.toString();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof GeoJsonMultiPoint)) {
            return false;
        }
        GeoJsonMultiPoint that = (GeoJsonMultiPoint) obj;
        return points.equals(that.points);
    }

    public int hashCode() {
        return points.hashCode();
    }
}
