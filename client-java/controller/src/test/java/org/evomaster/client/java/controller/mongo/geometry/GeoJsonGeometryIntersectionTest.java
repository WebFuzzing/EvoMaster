package org.evomaster.client.java.controller.mongo.geometry;

import org.evomaster.client.java.controller.mongo.MongoQueryTestCases;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

class GeoJsonGeometryIntersectionTest {

    private static final double DELTA = 1e-9;

    private static GeoJsonPoint point(double x, double y) {
        return new GeoJsonPoint(x, y);
    }

    private static GeoJsonLineString line(GeoJsonPoint... points) {
        return new GeoJsonLineString(Arrays.asList(points));
    }

    /** Closed axis-aligned square ring from (x0,y0) to (x1,y1). */
    private static GeoJsonLineRing squareRing(double x0, double y0, double x1, double y1) {
        return new GeoJsonLineRing(Arrays.asList(
                point(x0, y0), point(x1, y0), point(x1, y1), point(x0, y1), point(x0, y0)));
    }

    private static GeoJsonPolygon square(double x0, double y0, double x1, double y1) {
        return new GeoJsonPolygon(squareRing(x0, y0, x1, y1), Collections.emptySet());
    }

    /** Square [0,10]x[0,10] with a square hole [3,7]x[3,7]. */
    private static GeoJsonPolygon squareWithHole() {
        return new GeoJsonPolygon(squareRing(0, 0, 10, 10),
                new HashSet<>(Collections.singletonList(squareRing(3, 3, 7, 7))));
    }

    // ---------------------------------------------------------------------------------------------
    // distance: Point / Point
    // ---------------------------------------------------------------------------------------------

    @Test
    void testDistancePointPointSame() {
        assertEquals(0.0, GeoJsonGeometryIntersection.distance(point(10, 20), point(10, 20)), DELTA);
    }

    @Test
    void testDistancePointPointDifferent() {
        assertEquals(5.0, GeoJsonGeometryIntersection.distance(point(0, 0), point(3, 4)), DELTA);
    }

    // ---------------------------------------------------------------------------------------------
    // distance: Point / LineString
    // ---------------------------------------------------------------------------------------------

    @Test
    void testDistancePointOnLine() {
        GeoJsonLineString l = line(point(0, 0), point(10, 0));
        assertEquals(0.0, GeoJsonGeometryIntersection.distance(point(5, 0), l), DELTA);
    }

    @Test
    void testDistancePointPerpendicularToLine() {
        GeoJsonLineString l = line(point(0, 0), point(10, 0));
        assertEquals(3.0, GeoJsonGeometryIntersection.distance(point(5, 3), l), DELTA);
    }

    @Test
    void testDistancePointBeyondLineEndpoint() {
        GeoJsonLineString l = line(point(0, 0), point(10, 0));
        assertEquals(5.0, GeoJsonGeometryIntersection.distance(point(13, 4), l), DELTA);
    }

    @Test
    void testDistancePointToPolylineUsesClosestSegment() {
        GeoJsonLineString l = line(point(0, 0), point(10, 0), point(10, 10));
        assertEquals(2.0, GeoJsonGeometryIntersection.distance(point(12, 5), l), DELTA);
    }

    @Test
    void testDistanceLinePointIsSymmetric() {
        GeoJsonLineString l = line(point(0, 0), point(10, 0));
        GeoJsonPoint p = point(5, 3);
        assertEquals(GeoJsonGeometryIntersection.distance(p, l),
                GeoJsonGeometryIntersection.distance(l, p), DELTA);
    }

    // ---------------------------------------------------------------------------------------------
    // distance: Point / Polygon
    // ---------------------------------------------------------------------------------------------

    @Test
    void testDistancePointInsidePolygon() {
        assertEquals(0.0, GeoJsonGeometryIntersection.distance(point(2, 2), square(0, 0, 4, 4)), DELTA);
    }

    @Test
    void testDistancePointOnPolygonBoundary() {
        assertEquals(0.0, GeoJsonGeometryIntersection.distance(point(4, 2), square(0, 0, 4, 4)), DELTA);
    }

    @Test
    void testDistancePointOnPolygonVertex() {
        assertEquals(0.0, GeoJsonGeometryIntersection.distance(point(0, 0), square(0, 0, 4, 4)), DELTA);
    }

    @Test
    void testDistancePointOutsidePolygon() {
        assertEquals(2.0, GeoJsonGeometryIntersection.distance(point(6, 2), square(0, 0, 4, 4)), DELTA);
    }

    @Test
    void testDistancePointInsidePolygonHole() {
        assertEquals(2.0, GeoJsonGeometryIntersection.distance(point(5, 5), squareWithHole()), DELTA);
    }

    @Test
    void testDistancePointOnPolygonHoleBoundary() {
        assertEquals(0.0, GeoJsonGeometryIntersection.distance(point(3, 5), squareWithHole()), DELTA);
    }

    @Test
    void testDistancePointInPolygonFilledAreaBetweenHoleAndExterior() {
        assertEquals(0.0, GeoJsonGeometryIntersection.distance(point(1, 1), squareWithHole()), DELTA);
    }

    @Test
    void testDistancePolygonPointIsSymmetric() {
        GeoJsonPolygon polygon = square(0, 0, 4, 4);
        GeoJsonPoint p = point(6, 2);
        assertEquals(GeoJsonGeometryIntersection.distance(p, polygon),
                GeoJsonGeometryIntersection.distance(polygon, p), DELTA);
    }

    // ---------------------------------------------------------------------------------------------
    // distance: LineString / LineString
    // ---------------------------------------------------------------------------------------------

    @Test
    void testDistanceCrossingLines() {
        GeoJsonLineString a = line(point(0, 0), point(10, 10));
        GeoJsonLineString b = line(point(0, 10), point(10, 0));
        assertEquals(0.0, GeoJsonGeometryIntersection.distance(a, b), DELTA);
    }

    @Test
    void testDistanceLinesTouchingAtEndpoint() {
        GeoJsonLineString a = line(point(0, 0), point(5, 5));
        GeoJsonLineString b = line(point(5, 5), point(10, 0));
        assertEquals(0.0, GeoJsonGeometryIntersection.distance(a, b), DELTA);
    }

    @Test
    void testDistanceLineEndpointTouchingInteriorOfOther() {
        GeoJsonLineString a = line(point(0, 0), point(10, 0));
        GeoJsonLineString b = line(point(5, 0), point(5, 5));
        assertEquals(0.0, GeoJsonGeometryIntersection.distance(a, b), DELTA);
    }

    @Test
    void testDistanceCollinearOverlappingLines() {
        GeoJsonLineString a = line(point(0, 0), point(6, 0));
        GeoJsonLineString b = line(point(4, 0), point(10, 0));
        assertEquals(0.0, GeoJsonGeometryIntersection.distance(a, b), DELTA);
    }

    @Test
    void testDistanceCollinearDisjointLines() {
        GeoJsonLineString a = line(point(0, 0), point(4, 0));
        GeoJsonLineString b = line(point(7, 0), point(10, 0));
        assertEquals(3.0, GeoJsonGeometryIntersection.distance(a, b), DELTA);
    }

    @Test
    void testDistanceParallelLines() {
        GeoJsonLineString a = line(point(0, 0), point(10, 0));
        GeoJsonLineString b = line(point(0, 2), point(10, 2));
        assertEquals(2.0, GeoJsonGeometryIntersection.distance(a, b), DELTA);
    }

    @Test
    void testDistanceNonParallelDisjointLines() {
        // closest pair is the endpoint (5,1) of b to the segment a
        GeoJsonLineString a = line(point(0, 0), point(10, 0));
        GeoJsonLineString b = line(point(5, 1), point(6, 5));
        assertEquals(1.0, GeoJsonGeometryIntersection.distance(a, b), DELTA);
    }

    @Test
    void testDistancePolylinesCrossingOnLaterSegments() {
        GeoJsonLineString a = line(point(0, 0), point(1, 0), point(1, 10));
        GeoJsonLineString b = line(point(20, 20), point(20, 5), point(-5, 5));
        assertEquals(0.0, GeoJsonGeometryIntersection.distance(a, b), DELTA);
    }

    // ---------------------------------------------------------------------------------------------
    // distance: LineString / Polygon
    // ---------------------------------------------------------------------------------------------

    @Test
    void testDistanceLineInsidePolygon() {
        GeoJsonLineString l = line(point(1, 1), point(2, 2));
        assertEquals(0.0, GeoJsonGeometryIntersection.distance(l, square(0, 0, 4, 4)), DELTA);
    }

    @Test
    void testDistanceLineCrossingPolygonWithNoVertexInside() {
        GeoJsonLineString l = line(point(-1, 2), point(5, 2));
        assertEquals(0.0, GeoJsonGeometryIntersection.distance(l, square(0, 0, 4, 4)), DELTA);
    }

    @Test
    void testDistanceLineOutsidePolygon() {
        GeoJsonLineString l = line(point(6, 0), point(6, 4));
        assertEquals(2.0, GeoJsonGeometryIntersection.distance(l, square(0, 0, 4, 4)), DELTA);
    }

    @Test
    void testDistanceLineInsidePolygonHole() {
        GeoJsonLineString l = line(point(4, 5), point(6, 5));
        assertEquals(1.0, GeoJsonGeometryIntersection.distance(l, squareWithHole()), DELTA);
    }

    @Test
    void testDistanceLineCrossingPolygonHoleBoundary() {
        GeoJsonLineString l = line(point(5, 5), point(5, 6.5));
        assertEquals(0.5, GeoJsonGeometryIntersection.distance(l, squareWithHole()), DELTA);

        GeoJsonLineString crossing = line(point(5, 5), point(5, 8));
        assertEquals(0.0, GeoJsonGeometryIntersection.distance(crossing, squareWithHole()), DELTA);
    }

    @Test
    void testDistancePolygonLineIsSymmetric() {
        GeoJsonPolygon polygon = square(0, 0, 4, 4);
        GeoJsonLineString l = line(point(6, 0), point(6, 4));
        assertEquals(GeoJsonGeometryIntersection.distance(l, polygon),
                GeoJsonGeometryIntersection.distance(polygon, l), DELTA);
    }

    // ---------------------------------------------------------------------------------------------
    // distance: Polygon / Polygon
    // ---------------------------------------------------------------------------------------------

    @Test
    void testDistanceOverlappingPolygons() {
        assertEquals(0.0, GeoJsonGeometryIntersection.distance(square(0, 0, 4, 4), square(2, 2, 6, 6)), DELTA);
    }

    @Test
    void testDistancePolygonContainedInOther() {
        GeoJsonPolygon outer = square(0, 0, 10, 10);
        GeoJsonPolygon inner = square(2, 2, 3, 3);
        assertEquals(0.0, GeoJsonGeometryIntersection.distance(outer, inner), DELTA);
        assertEquals(0.0, GeoJsonGeometryIntersection.distance(inner, outer), DELTA);
    }

    @Test
    void testDistancePolygonsCrossingWithNoVertexInside() {
        // a horizontal bar and a vertical bar forming a cross: no vertex of either lies in the other
        GeoJsonPolygon horizontal = square(0, 0, 4, 1);
        GeoJsonPolygon vertical = square(1, -1, 2, 2);
        assertEquals(0.0, GeoJsonGeometryIntersection.distance(horizontal, vertical), DELTA);
    }

    @Test
    void testDistancePolygonsSharingEdge() {
        assertEquals(0.0, GeoJsonGeometryIntersection.distance(square(0, 0, 4, 4), square(4, 0, 8, 4)), DELTA);
    }

    @Test
    void testDistanceDisjointPolygons() {
        assertEquals(3.0, GeoJsonGeometryIntersection.distance(square(0, 0, 4, 4), square(7, 0, 10, 4)), DELTA);
    }

    @Test
    void testDistancePolygonInsideHoleOfOther() {
        GeoJsonPolygon island = square(4, 4, 6, 6);
        assertEquals(1.0, GeoJsonGeometryIntersection.distance(island, squareWithHole()), DELTA);
        assertEquals(1.0, GeoJsonGeometryIntersection.distance(squareWithHole(), island), DELTA);
    }

    @Test
    void testDistancePolygonCrossingHoleBoundaryOfOther() {
        GeoJsonPolygon crossing = square(4, 4, 8, 6);
        assertEquals(0.0, GeoJsonGeometryIntersection.distance(crossing, squareWithHole()), DELTA);
        assertEquals(0.0, GeoJsonGeometryIntersection.distance(squareWithHole(), crossing), DELTA);
    }

    // ---------------------------------------------------------------------------------------------
    // distance: Multi* and GeometryCollection
    // ---------------------------------------------------------------------------------------------

    @Test
    void testDistanceMultiPointUsesClosestPoint() {
        GeoJsonMultiPoint mp = new GeoJsonMultiPoint(Arrays.asList(point(50, 50), point(0, 3), point(-30, -40)));
        assertEquals(3.0, GeoJsonGeometryIntersection.distance(mp, point(0, 0)), DELTA);
    }

    @Test
    void testDistanceMultiPointIntersecting() {
        GeoJsonMultiPoint mp = new GeoJsonMultiPoint(Arrays.asList(point(10, 20), point(50, 50)));
        assertEquals(0.0, GeoJsonGeometryIntersection.distance(mp, point(10, 20)), DELTA);
    }

    @Test
    void testDistanceMultiLineString() {
        GeoJsonMultiLineString mls = new GeoJsonMultiLineString(Arrays.asList(
                line(point(0, 10), point(10, 10)),
                line(point(0, 0), point(10, 0))));
        assertEquals(1.0, GeoJsonGeometryIntersection.distance(mls, point(5, 1)), DELTA);
    }

    @Test
    void testDistanceMultiPolygon() {
        GeoJsonMultiPolygon mpoly = new GeoJsonMultiPolygon(Arrays.asList(
                square(0, 0, 1, 1), square(10, 10, 11, 11)));
        assertEquals(0.0, GeoJsonGeometryIntersection.distance(mpoly, point(10.5, 10.5)), DELTA);
        assertEquals(1.0, GeoJsonGeometryIntersection.distance(mpoly, point(2, 0.5)), DELTA);
    }

    @Test
    void testDistanceBetweenTwoMultiGeometries() {
        GeoJsonMultiPoint a = new GeoJsonMultiPoint(Arrays.asList(point(0, 0), point(20, 20)));
        GeoJsonMultiPoint b = new GeoJsonMultiPoint(Arrays.asList(point(40, 40), point(23, 24)));
        assertEquals(5.0, GeoJsonGeometryIntersection.distance(a, b), DELTA);
    }

    @Test
    void testDistanceGeometryCollection() {
        GeoJsonGeometryCollection gc = new GeoJsonGeometryCollection(Arrays.asList(
                point(50, 50),
                line(point(0, 10), point(10, 10))));
        assertEquals(2.0, GeoJsonGeometryIntersection.distance(gc, point(5, 12)), DELTA);
    }

    @Test
    void testDistanceNestedGeometryCollection() {
        GeoJsonGeometryCollection inner = new GeoJsonGeometryCollection(
                Collections.singletonList(square(0, 0, 4, 4)));
        GeoJsonGeometryCollection outer = new GeoJsonGeometryCollection(Arrays.asList(point(50, 50), inner));
        assertEquals(0.0, GeoJsonGeometryIntersection.distance(outer, point(2, 2)), DELTA);
    }

    @Test
    void testDistanceEmptyGeometryCollection() {
        GeoJsonGeometryCollection empty = new GeoJsonGeometryCollection(Collections.emptyList());
        assertEquals(Double.MAX_VALUE, GeoJsonGeometryIntersection.distance(empty, point(0, 0)));
        assertEquals(Double.MAX_VALUE, GeoJsonGeometryIntersection.distance(point(0, 0), empty));
    }

    @Test
    void testDistanceEmptyMultiPoint() {
        GeoJsonMultiPoint empty = new GeoJsonMultiPoint(Collections.emptyList());
        assertEquals(Double.MAX_VALUE, GeoJsonGeometryIntersection.distance(empty, point(0, 0)));
    }

    // ---------------------------------------------------------------------------------------------
    // distanceToContainment
    // ---------------------------------------------------------------------------------------------

    @Test
    void testContainmentPointInsidePolygon() {
        assertEquals(0.0, GeoJsonGeometryIntersection.distanceToContainment(point(2, 2), square(0, 0, 4, 4)), DELTA);
    }

    @Test
    void testContainmentPointOnPolygonBoundary() {
        assertEquals(0.0, GeoJsonGeometryIntersection.distanceToContainment(point(0, 2), square(0, 0, 4, 4)), DELTA);
    }

    @Test
    void testContainmentPointOutsidePolygon() {
        assertEquals(2.0, GeoJsonGeometryIntersection.distanceToContainment(point(6, 2), square(0, 0, 4, 4)), DELTA);
    }

    @Test
    void testContainmentPointInsideHole() {
        assertEquals(2.0, GeoJsonGeometryIntersection.distanceToContainment(point(5, 5), squareWithHole()), DELTA);
    }

    @Test
    void testContainmentLineFullyInside() {
        GeoJsonLineString l = line(point(1, 1), point(3, 3));
        assertEquals(0.0, GeoJsonGeometryIntersection.distanceToContainment(l, square(0, 0, 4, 4)), DELTA);
    }

    @Test
    void testContainmentLinePartiallyOutsideUsesFarthestVertex() {
        GeoJsonLineString l = line(point(1, 1), point(5, 1), point(7, 1));
        assertEquals(3.0, GeoJsonGeometryIntersection.distanceToContainment(l, square(0, 0, 4, 4)), DELTA);
    }

    @Test
    void testContainmentPolygonInsidePolygon() {
        assertEquals(0.0, GeoJsonGeometryIntersection.distanceToContainment(square(1, 1, 2, 2), square(0, 0, 4, 4)), DELTA);
    }

    @Test
    void testContainmentPolygonInsideHole() {
        assertEquals(1.0, GeoJsonGeometryIntersection.distanceToContainment(square(4, 4, 6, 6), squareWithHole()), DELTA);
    }

    @Test
    void testContainmentPolygonWithHoleChecksAllRingVertices() {
        // hole vertices are inside the area, the farthest exterior vertices (6,1) and (6,3) are 2 away
        GeoJsonPolygon actual = new GeoJsonPolygon(squareRing(1, 1, 6, 3),
                new HashSet<>(Collections.singletonList(squareRing(2, 1.5, 3, 2.5))));
        assertEquals(2.0, GeoJsonGeometryIntersection.distanceToContainment(actual, square(0, 0, 4, 4)), DELTA);
    }

    @Test
    void testContainmentMultiPointWithOneOutside() {
        GeoJsonMultiPoint mp = new GeoJsonMultiPoint(Arrays.asList(point(1, 1), point(2, 2), point(4, 8)));
        assertEquals(4.0, GeoJsonGeometryIntersection.distanceToContainment(mp, square(0, 0, 4, 4)), DELTA);
    }

    @Test
    void testContainmentMultiLineString() {
        GeoJsonMultiLineString mls = new GeoJsonMultiLineString(Arrays.asList(
                line(point(1, 1), point(2, 2)),
                line(point(1, 3), point(1, 5))));
        assertEquals(1.0, GeoJsonGeometryIntersection.distanceToContainment(mls, square(0, 0, 4, 4)), DELTA);
    }

    @Test
    void testContainmentInMultiPolygon() {
        GeoJsonMultiPolygon area = new GeoJsonMultiPolygon(Arrays.asList(
                square(0, 0, 1, 1), square(10, 10, 11, 11)));
        assertEquals(0.0, GeoJsonGeometryIntersection.distanceToContainment(point(10.5, 10.5), area), DELTA);
        assertEquals(4.0, GeoJsonGeometryIntersection.distanceToContainment(point(5, 0.5), area), DELTA);
    }

    @Test
    void testContainmentPointsSpreadAcrossMultiPolygon() {
        GeoJsonMultiPolygon area = new GeoJsonMultiPolygon(Arrays.asList(
                square(0, 0, 1, 1), square(10, 10, 11, 11)));
        GeoJsonMultiPoint mp = new GeoJsonMultiPoint(Arrays.asList(point(0.5, 0.5), point(10.5, 10.5)));
        assertEquals(0.0, GeoJsonGeometryIntersection.distanceToContainment(mp, area), DELTA);
    }

    @Test
    void testContainmentInGeometryCollectionIgnoresNonAreaMembers() {
        GeoJsonGeometryCollection area = new GeoJsonGeometryCollection(Arrays.asList(
                point(6, 2),
                line(point(6, 0), point(6, 4)),
                square(0, 0, 4, 4)));
        assertEquals(2.0, GeoJsonGeometryIntersection.distanceToContainment(point(6, 2), area), DELTA);
        assertEquals(0.0, GeoJsonGeometryIntersection.distanceToContainment(point(1, 1), area), DELTA);
    }

    @Test
    void testContainmentActualGeometryCollection() {
        GeoJsonGeometryCollection actual = new GeoJsonGeometryCollection(Arrays.asList(
                point(1, 1),
                line(point(2, 2), point(3, 6))));
        assertEquals(2.0, GeoJsonGeometryIntersection.distanceToContainment(actual, square(0, 0, 4, 4)), DELTA);
    }

    @Test
    void testContainmentInNonAreaGeometry() {
        GeoJsonLineString area = line(point(0, 0), point(10, 0));
        assertEquals(Double.MAX_VALUE, GeoJsonGeometryIntersection.distanceToContainment(point(5, 0), area));
    }

    @Test
    void testContainmentOfEmptyGeometry() {
        GeoJsonGeometryCollection empty = new GeoJsonGeometryCollection(Collections.emptyList());
        assertEquals(Double.MAX_VALUE, GeoJsonGeometryIntersection.distanceToContainment(empty, square(0, 0, 4, 4)));
    }

    @Test
    void testDistanceBetweenDetailedDisjointLinesFinishesWithinReviewThreshold() {
        // Initialize the distance calculation before measuring the larger fixture.
        assertEquals(1.0, GeoJsonGeometryIntersection.distance(detailedLine(16, 0), detailedLine(16, 1)));

        GeoJsonLineString first = detailedLine(2000, 0);
        GeoJsonLineString second = detailedLine(2000, 1);

        // MongoDB accepts these fixtures: the parallel lines do not intersect, while
        // each line intersects itself. The shared live-Mongo oracle checks that too.
        // An array-backed traversal takes tens of milliseconds locally; two seconds
        // leaves substantial headroom. This is a review threshold, not a product SLA.
        // Indexed LinkedList traversal adds a cubic
        // cost and takes several seconds for this otherwise modest geometry.
        // Use a synchronous timeout so a failure never leaves a worker running.
        double distance = assertTimeout(Duration.ofSeconds(2),
                () -> GeoJsonGeometryIntersection.distance(first, second));

        assertEquals(1.0, distance);
    }

    private static GeoJsonLineString detailedLine(int size, double latitude) {
        return GeoJsonUtils.toGeoJsonLineString(
                MongoQueryTestCases.detailedLine(size, latitude));
    }
}
