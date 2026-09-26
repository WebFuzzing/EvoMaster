package org.evomaster.client.java.controller.mongo.geometry;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

public class GeoJsonPolygon extends GeoJsonGeometry {

    public static final String POLYGON_TYPE = "Polygon";

    private final GeoJsonLineRing exteriorRing;

    private final Set<GeoJsonLineRing> interiorRings;

    public GeoJsonPolygon(GeoJsonLineRing exteriorRing, Set<GeoJsonLineRing> interiorRings) {
        super(POLYGON_TYPE);
        // TODO check that the interior rings are inside the exterior ring and do not intersect each other
        this.exteriorRing = Objects.requireNonNull(exteriorRing);
        this.interiorRings = Collections.unmodifiableSet(Objects.requireNonNull(interiorRings));
    }

    public GeoJsonLineRing getExteriorRing() {
        return exteriorRing;
    }

    public Set<GeoJsonLineRing> getInteriorRings() {
        return interiorRings;
    }

    @Override
    public boolean hasArea() {
        return true;
    }

    public String toString() {
        return "GeoJsonPolygon{" +
                "exteriorRing=" + exteriorRing +
                ", interiorRings=" + interiorRings +
                '}';
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof GeoJsonPolygon)) {
            return false;
        }
        GeoJsonPolygon that = (GeoJsonPolygon) obj;
        return exteriorRing.equals(that.exteriorRing) && interiorRings.equals(that.interiorRings);
    }

    public int hashCode() {
        return Objects.hash(exteriorRing, interiorRings);
    }
}
