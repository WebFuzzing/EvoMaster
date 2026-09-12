package org.evomaster.client.java.controller.mongo.geometry;

import org.evomaster.client.java.controller.mongo.utils.MongoUtils;

import java.util.ArrayList;
import java.util.List;

import static org.evomaster.client.java.controller.mongo.utils.MongoUtils.GeoSpatialModel.PLANAR;

/**
 * Computes approximate planar distances between GeoJSON geometries, used as branch-distance
 * heuristics for {@code $geoIntersects} ({@link #distance}) and {@code $geoWithin}
 * ({@link #distanceToContainment}) queries, treating longitude/latitude as planar Cartesian
 * coordinates. This intentionally ignores MongoDB's spherical GeoJSON semantics: it is a
 * simplification adequate for guiding a search-based test generator, not for exact geometric
 * computation.
 */
public abstract class GeoJsonGeometryIntersection {

    public static double distance(GeoJsonGeometry a, GeoJsonGeometry b) {
        List<Shape> shapesA = flatten(a);
        List<Shape> shapesB = flatten(b);
        if (shapesA.isEmpty() || shapesB.isEmpty()) {
            return Double.MAX_VALUE;
        }

        double min = Double.MAX_VALUE;
        for (Shape shapeA : shapesA) {
            for (Shape shapeB : shapesB) {
                double d = distance(shapeA, shapeB);
                if (d < min) {
                    min = d;
                }
                if (min <= 0.0) {
                    return 0.0;
                }
            }
        }
        return min;
    }

    /**
     * Distance from full containment: 0 when every point of {@code actual} lies within (or on the
     * boundary of) the area(s) enclosed by {@code area}, otherwise the distance of the
     * farthest-outside point of {@code actual} from that area. Used as a branch-distance heuristic
     * for {@code $geoWithin}. {@code area} must be a geometry with {@link GeoJsonGeometry#hasArea()}.
     */
    public static double distanceToContainment(GeoJsonGeometry actual, GeoJsonGeometry area) {
        List<GeoJsonPoint> points = new ArrayList<>();
        collectPoints(actual, points);
        if (points.isEmpty()) {
            return Double.MAX_VALUE;
        }

        double max = 0.0;
        for (GeoJsonPoint point : points) {
            double d = distanceOutsideArea(point, area);
            if (d > max) {
                max = d;
            }
        }
        return max;
    }

    private static void collectPoints(GeoJsonGeometry geometry, List<GeoJsonPoint> points) {
        if (geometry instanceof GeoJsonPoint) {
            points.add((GeoJsonPoint) geometry);
        } else if (geometry instanceof GeoJsonLineString) {
            points.addAll(((GeoJsonLineString) geometry).getPoints());
        } else if (geometry instanceof GeoJsonPolygon) {
            collectPolygonPoints((GeoJsonPolygon) geometry, points);
        } else if (geometry instanceof GeoJsonMultiPoint) {
            points.addAll(((GeoJsonMultiPoint) geometry).getPoints());
        } else if (geometry instanceof GeoJsonMultiLineString) {
            for (GeoJsonLineString line : ((GeoJsonMultiLineString) geometry).getLineStrings()) {
                points.addAll(line.getPoints());
            }
        } else if (geometry instanceof GeoJsonMultiPolygon) {
            for (GeoJsonPolygon polygon : ((GeoJsonMultiPolygon) geometry).getPolygons()) {
                collectPolygonPoints(polygon, points);
            }
        } else if (geometry instanceof GeoJsonGeometryCollection) {
            for (GeoJsonGeometry inner : ((GeoJsonGeometryCollection) geometry).getGeometries()) {
                collectPoints(inner, points);
            }
        }
    }

    private static void collectPolygonPoints(GeoJsonPolygon polygon, List<GeoJsonPoint> points) {
        points.addAll(polygon.getExteriorRing().getPoints());
        for (GeoJsonLineRing hole : polygon.getInteriorRings()) {
            points.addAll(hole.getPoints());
        }
    }

    /** Distance of a point outside the area(s) enclosed by a geometry; 0 if it lies within (or on) one. */
    private static double distanceOutsideArea(GeoJsonPoint point, GeoJsonGeometry area) {
        if (area instanceof GeoJsonPolygon) {
            GeoJsonPolygon polygon = (GeoJsonPolygon) area;
            return distancePointPolygon(point, polygon.getExteriorRing().getPoints(), polygonHoles(polygon));
        } else if (area instanceof GeoJsonMultiPolygon) {
            double min = Double.MAX_VALUE;
            for (GeoJsonPolygon polygon : ((GeoJsonMultiPolygon) area).getPolygons()) {
                double d = distancePointPolygon(point, polygon.getExteriorRing().getPoints(), polygonHoles(polygon));
                if (d < min) {
                    min = d;
                }
            }
            return min;
        } else if (area instanceof GeoJsonGeometryCollection) {
            double min = Double.MAX_VALUE;
            for (GeoJsonGeometry inner : ((GeoJsonGeometryCollection) area).getGeometries()) {
                if (inner.hasArea()) {
                    double d = distanceOutsideArea(point, inner);
                    if (d < min) {
                        min = d;
                    }
                }
            }
            return min;
        }
        return Double.MAX_VALUE;
    }

    private static List<List<GeoJsonPoint>> polygonHoles(GeoJsonPolygon polygon) {
        List<List<GeoJsonPoint>> holes = new ArrayList<>();
        for (GeoJsonLineRing hole : polygon.getInteriorRings()) {
            holes.add(hole.getPoints());
        }
        return holes;
    }

    private enum Kind {POINT, LINE, POLYGON}

    /** A single Point, an open polyline (LineString), or a Polygon (exterior ring plus holes). */
    private static final class Shape {
        final Kind kind;
        final GeoJsonPoint point;
        final List<GeoJsonPoint> line;
        final List<GeoJsonPoint> exteriorRing;
        final List<List<GeoJsonPoint>> interiorRings;

        private Shape(GeoJsonPoint point) {
            this.kind = Kind.POINT;
            this.point = point;
            this.line = null;
            this.exteriorRing = null;
            this.interiorRings = null;
        }

        private Shape(List<GeoJsonPoint> line) {
            this.kind = Kind.LINE;
            this.point = null;
            this.line = line;
            this.exteriorRing = null;
            this.interiorRings = null;
        }

        private Shape(List<GeoJsonPoint> exteriorRing, List<List<GeoJsonPoint>> interiorRings) {
            this.kind = Kind.POLYGON;
            this.point = null;
            this.line = null;
            this.exteriorRing = exteriorRing;
            this.interiorRings = interiorRings;
        }
    }

    private static List<Shape> flatten(GeoJsonGeometry geometry) {
        List<Shape> shapes = new ArrayList<>();
        collect(geometry, shapes);
        return shapes;
    }

    private static void collect(GeoJsonGeometry geometry, List<Shape> shapes) {
        if (geometry instanceof GeoJsonPoint) {
            shapes.add(new Shape((GeoJsonPoint) geometry));
        } else if (geometry instanceof GeoJsonLineString) {
            shapes.add(new Shape(((GeoJsonLineString) geometry).getPoints()));
        } else if (geometry instanceof GeoJsonPolygon) {
            shapes.add(toPolygonShape((GeoJsonPolygon) geometry));
        } else if (geometry instanceof GeoJsonMultiPoint) {
            for (GeoJsonPoint point : ((GeoJsonMultiPoint) geometry).getPoints()) {
                shapes.add(new Shape(point));
            }
        } else if (geometry instanceof GeoJsonMultiLineString) {
            for (GeoJsonLineString line : ((GeoJsonMultiLineString) geometry).getLineStrings()) {
                shapes.add(new Shape(line.getPoints()));
            }
        } else if (geometry instanceof GeoJsonMultiPolygon) {
            for (GeoJsonPolygon polygon : ((GeoJsonMultiPolygon) geometry).getPolygons()) {
                shapes.add(toPolygonShape(polygon));
            }
        } else if (geometry instanceof GeoJsonGeometryCollection) {
            for (GeoJsonGeometry inner : ((GeoJsonGeometryCollection) geometry).getGeometries()) {
                collect(inner, shapes);
            }
        }
    }

    private static Shape toPolygonShape(GeoJsonPolygon polygon) {
        List<List<GeoJsonPoint>> holes = new ArrayList<>();
        for (GeoJsonLineRing hole : polygon.getInteriorRings()) {
            holes.add(hole.getPoints());
        }
        return new Shape(polygon.getExteriorRing().getPoints(), holes);
    }

    private static double distance(Shape a, Shape b) {
        switch (a.kind) {
            case POINT:
                switch (b.kind) {
                    case POINT:
                        return distancePointPoint(a.point, b.point);
                    case LINE:
                        return distancePointPolyline(a.point, b.line);
                    case POLYGON:
                        return distancePointPolygon(a.point, b.exteriorRing, b.interiorRings);
                }
                break;
            case LINE:
                switch (b.kind) {
                    case POINT:
                        return distancePointPolyline(b.point, a.line);
                    case LINE:
                        return distancePolylinePolyline(a.line, b.line);
                    case POLYGON:
                        return distancePolylinePolygon(a.line, b.exteriorRing, b.interiorRings);
                }
                break;
            case POLYGON:
                switch (b.kind) {
                    case POINT:
                        return distancePointPolygon(b.point, a.exteriorRing, a.interiorRings);
                    case LINE:
                        return distancePolylinePolygon(b.line, a.exteriorRing, a.interiorRings);
                    case POLYGON:
                        return distancePolygonPolygon(a, b);
                }
                break;
        }
        throw new IllegalStateException("Unreachable: unknown shape kind combination");
    }

    private static double distancePointPoint(GeoJsonPoint p1, GeoJsonPoint p2) {
        return MongoUtils.getDistanceBetweenPoints(
                p1.getLongitude(), p1.getLatitude(),
                p2.getLongitude(), p2.getLatitude(), PLANAR);
    }

    private static double distancePointSegment(GeoJsonPoint point, GeoJsonPoint segStart, GeoJsonPoint segEnd) {
        double px = point.getLongitude(), py = point.getLatitude();
        double x1 = segStart.getLongitude(), y1 = segStart.getLatitude();
        double x2 = segEnd.getLongitude(), y2 = segEnd.getLatitude();

        double dx = x2 - x1;
        double dy = y2 - y1;
        if (dx == 0 && dy == 0) {
            return distancePointPoint(point, segStart);
        }
        double t = ((px - x1) * dx + (py - y1) * dy) / (dx * dx + dy * dy);
        t = Math.max(0.0, Math.min(1.0, t));
        double closestX = x1 + t * dx;
        double closestY = y1 + t * dy;
        double ddx = px - closestX;
        double ddy = py - closestY;
        return Math.sqrt(ddx * ddx + ddy * ddy);
    }

    private static double distancePointPolyline(GeoJsonPoint point, List<GeoJsonPoint> path) {
        double min = Double.MAX_VALUE;
        for (int i = 0; i < path.size() - 1; i++) {
            double d = distancePointSegment(point, path.get(i), path.get(i + 1));
            if (d < min) {
                min = d;
            }
        }
        return min;
    }

    /**
     * Distance from a point to a polygon: 0 if the point touches the boundary or lies in the
     * filled area (inside the exterior ring and outside every hole); otherwise, the distance to
     * the closest boundary (exterior ring or hole).
     */
    private static double distancePointPolygon(GeoJsonPoint point, List<GeoJsonPoint> exteriorRing,
                                                List<List<GeoJsonPoint>> interiorRings) {
        double boundaryDistance = distancePointPolyline(point, exteriorRing);
        for (List<GeoJsonPoint> hole : interiorRings) {
            boundaryDistance = Math.min(boundaryDistance, distancePointPolyline(point, hole));
        }
        if (boundaryDistance <= 0.0) {
            return 0.0;
        }

        if (!rayCast(point, exteriorRing)) {
            return boundaryDistance;
        }
        for (List<GeoJsonPoint> hole : interiorRings) {
            if (rayCast(point, hole)) {
                return boundaryDistance;
            }
        }
        return 0.0;
    }

    /** Even-odd ray casting point-in-polygon test. The ring's last point must repeat the first. */
    private static boolean rayCast(GeoJsonPoint point, List<GeoJsonPoint> ring) {
        double px = point.getLongitude();
        double py = point.getLatitude();
        boolean inside = false;
        int n = ring.size() - 1;
        for (int i = 0, j = n - 1; i < n; j = i++) {
            double xi = ring.get(i).getLongitude(), yi = ring.get(i).getLatitude();
            double xj = ring.get(j).getLongitude(), yj = ring.get(j).getLatitude();
            boolean crosses = ((yi > py) != (yj > py))
                    && (px < (xj - xi) * (py - yi) / (yj - yi) + xi);
            if (crosses) {
                inside = !inside;
            }
        }
        return inside;
    }

    private static double distancePolylinePolyline(List<GeoJsonPoint> a, List<GeoJsonPoint> b) {
        double min = Double.MAX_VALUE;
        for (int i = 0; i < a.size() - 1; i++) {
            for (int j = 0; j < b.size() - 1; j++) {
                double d = distanceSegmentSegment(a.get(i), a.get(i + 1), b.get(j), b.get(j + 1));
                if (d < min) {
                    min = d;
                }
                if (min <= 0.0) {
                    return 0.0;
                }
            }
        }
        return min;
    }

    private static double distanceSegmentSegment(GeoJsonPoint p1, GeoJsonPoint p2, GeoJsonPoint p3, GeoJsonPoint p4) {
        if (segmentsIntersect(p1, p2, p3, p4)) {
            return 0.0;
        }
        return Math.min(
                Math.min(distancePointSegment(p1, p3, p4), distancePointSegment(p2, p3, p4)),
                Math.min(distancePointSegment(p3, p1, p2), distancePointSegment(p4, p1, p2))
        );
    }

    private static double cross(GeoJsonPoint o, GeoJsonPoint a, GeoJsonPoint b) {
        return (a.getLongitude() - o.getLongitude()) * (b.getLatitude() - o.getLatitude())
                - (a.getLatitude() - o.getLatitude()) * (b.getLongitude() - o.getLongitude());
    }

    private static boolean onSegment(GeoJsonPoint p, GeoJsonPoint segStart, GeoJsonPoint segEnd) {
        return Math.min(segStart.getLongitude(), segEnd.getLongitude()) <= p.getLongitude()
                && p.getLongitude() <= Math.max(segStart.getLongitude(), segEnd.getLongitude())
                && Math.min(segStart.getLatitude(), segEnd.getLatitude()) <= p.getLatitude()
                && p.getLatitude() <= Math.max(segStart.getLatitude(), segEnd.getLatitude());
    }

    /** Standard orientation-based segment intersection test, including collinear touching/overlap. */
    private static boolean segmentsIntersect(GeoJsonPoint p1, GeoJsonPoint p2, GeoJsonPoint p3, GeoJsonPoint p4) {
        double d1 = cross(p3, p4, p1);
        double d2 = cross(p3, p4, p2);
        double d3 = cross(p1, p2, p3);
        double d4 = cross(p1, p2, p4);

        if (((d1 > 0 && d2 < 0) || (d1 < 0 && d2 > 0))
                && ((d3 > 0 && d4 < 0) || (d3 < 0 && d4 > 0))) {
            return true;
        }
        if (d1 == 0 && onSegment(p1, p3, p4)) {
            return true;
        }
        if (d2 == 0 && onSegment(p2, p3, p4)) {
            return true;
        }
        if (d3 == 0 && onSegment(p3, p1, p2)) {
            return true;
        }
        return d4 == 0 && onSegment(p4, p1, p2);
    }

    private static double distancePolylinePolygon(List<GeoJsonPoint> line, List<GeoJsonPoint> exteriorRing,
                                                   List<List<GeoJsonPoint>> interiorRings) {
        double min = Double.MAX_VALUE;
        for (GeoJsonPoint vertex : line) {
            double d = distancePointPolygon(vertex, exteriorRing, interiorRings);
            if (d < min) {
                min = d;
            }
            if (min <= 0.0) {
                return 0.0;
            }
        }
        double d = distancePolylinePolyline(line, exteriorRing);
        if (d < min) {
            min = d;
        }
        if (min <= 0.0) {
            return 0.0;
        }
        for (List<GeoJsonPoint> hole : interiorRings) {
            d = distancePolylinePolyline(line, hole);
            if (d < min) {
                min = d;
            }
            if (min <= 0.0) {
                return 0.0;
            }
        }
        return min;
    }

    private static double distancePolygonPolygon(Shape a, Shape b) {
        double min = Double.MAX_VALUE;
        for (GeoJsonPoint vertex : a.exteriorRing) {
            double d = distancePointPolygon(vertex, b.exteriorRing, b.interiorRings);
            if (d < min) {
                min = d;
            }
            if (min <= 0.0) {
                return 0.0;
            }
        }
        for (GeoJsonPoint vertex : b.exteriorRing) {
            double d = distancePointPolygon(vertex, a.exteriorRing, a.interiorRings);
            if (d < min) {
                min = d;
            }
            if (min <= 0.0) {
                return 0.0;
            }
        }
        double d = distancePolylinePolyline(a.exteriorRing, b.exteriorRing);
        if (d < min) {
            min = d;
        }
        if (min <= 0.0) {
            return 0.0;
        }
        for (List<GeoJsonPoint> hole : a.interiorRings) {
            d = distancePolylinePolyline(hole, b.exteriorRing);
            if (d < min) {
                min = d;
            }
            if (min <= 0.0) {
                return 0.0;
            }
        }
        for (List<GeoJsonPoint> hole : b.interiorRings) {
            d = distancePolylinePolyline(a.exteriorRing, hole);
            if (d < min) {
                min = d;
            }
            if (min <= 0.0) {
                return 0.0;
            }
        }
        return min;
    }
}
