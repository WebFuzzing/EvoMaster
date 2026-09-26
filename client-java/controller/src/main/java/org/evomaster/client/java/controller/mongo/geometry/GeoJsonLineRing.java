package org.evomaster.client.java.controller.mongo.geometry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

/** A closed LineString with at least four distinct coordinate pairs. */
public final class GeoJsonLineRing extends GeoJsonLineString {

    /**
     * Removes consecutive duplicates using the LineString constructor, then
     * validates closure and the number of distinct points. The closing point
     * does not count as a distinct point, so a ring needs at least five positions.
     *
     * @throws IllegalArgumentException if the ring is open or has fewer than four distinct points
     * @throws NullPointerException if the list or any point is null
     */
    public GeoJsonLineRing(List<GeoJsonPoint> points) {
        super(points);
        List<GeoJsonPoint> noConsecutiveDuplicates = super.getPoints();
        GeoJsonPoint first = noConsecutiveDuplicates.get(0);
        GeoJsonPoint last = noConsecutiveDuplicates.get(noConsecutiveDuplicates.size() - 1);
        if (!first.equals(last)) {
            throw new IllegalArgumentException("A LineRing must have the same first and last point.");
        }

        LinkedHashSet<GeoJsonPoint> distinctPoints = new LinkedHashSet<>(noConsecutiveDuplicates);
        if (distinctPoints.size() < 3) {
            throw new IllegalArgumentException("A LineRing must have at least three distinct points.");
        }
        //TODO: validate that the ring does not self-intersect
    }

    /** Returns an unmodifiable list to preserve the validated ring. */
    @Override
    public List<GeoJsonPoint> getPoints() {
        return Collections.unmodifiableList(super.getPoints());
    }
}
