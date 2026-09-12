package org.evomaster.client.java.controller.mongo.geometry;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class GeoJsonLineString extends GeoJsonObject {

    public static final String LINE_STRING_TYPE = "LineString";

    private final List<GeoJsonPoint> points;

    /**
     * Constructs a GeoJsonLineString object with a list of GeoJsonPoint objects.
     * The constructor ensures that the list of points is not null, removes consecutive duplicate points,
     * and checks that there are at least two distinct points in the list.
     *
     * @param points the list of GeoJsonPoint objects representing the LineString
     */
    public GeoJsonLineString(List<GeoJsonPoint> points) {
        super(LINE_STRING_TYPE);
        Objects.requireNonNull(points);
        this.points = removeConsecutiveDuplicates(points);
        if (this.points.size() < 2) {
            throw new IllegalArgumentException("A LineString must have at least two distinct points.");
        }
    }

    private List<GeoJsonPoint> removeConsecutiveDuplicates(List<GeoJsonPoint> points) {
        final List<GeoJsonPoint> result = new LinkedList<>();
        GeoJsonPoint previousPoint = null;
        for (GeoJsonPoint point : points) {
            Objects.requireNonNull(point, "Points in the LineString cannot be null.");
            if (previousPoint == null
                    || point.getLongitude() != previousPoint.getLongitude()
                    || point.getLatitude() != previousPoint.getLatitude()) {
                result.add(point);
            }
            previousPoint = point;
        }
        return result;
    }

    public List<GeoJsonPoint> getPoints() {
        return points;
    }

    public String toString() {
        return "GeoJsonLineString{" +
                "points=" + points +
                '}';
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof GeoJsonLineString)) {
            return false;
        }
        GeoJsonLineString that = (GeoJsonLineString) obj;
        return points.equals(that.points);
    }

    public int hashCode() {
        return Objects.hash(points);
    }
}
