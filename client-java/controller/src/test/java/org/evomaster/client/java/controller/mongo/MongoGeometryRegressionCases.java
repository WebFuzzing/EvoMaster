package org.evomaster.client.java.controller.mongo;

import org.bson.Document;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/** Shared fixtures: every expected match is checked against a live server by MongoQueryOracleIT. */
public final class MongoGeometryRegressionCases {

    private MongoGeometryRegressionCases() {
    }

    static Stream<MongoQueryCase> scenarios() {
        List<MongoQueryCase> cases = new ArrayList<>();
        Document square = geometry("Polygon", "[[[0,0],[10,0],[10,10],[0,10],[0,0]]]");
        Document donut = geometry("Polygon",
                "[[[0,0],[10,0],[10,10],[0,10],[0,0]],[[4,4],[4,6],[6,6],[6,4],[4,4]]]");
        Document inside = geometry("Point", "[1,1]");
        Document outside = geometry("Point", "[20,20]");

        add(cases, "line crosses polygon hole", geo("$geoWithin", donut),
                geometry("LineString", "[[2,5],[8,5]]"), false);
        add(cases, "polygon covers excluded hole", geo("$geoWithin", donut),
                geometry("Polygon", "[[[2,2],[8,2],[8,8],[2,8],[2,2]]]"), false);
        add(cases, "line stays in filled area", geo("$geoWithin", donut),
                geometry("LineString", "[[1,1],[3,1]]"), true);
        add(cases, "point lies in excluded hole", geo("$geoWithin", donut),
                geometry("Point", "[5,5]"), false);
        Document islands = geometry("MultiPolygon",
                "[[[[0,0],[2,0],[2,2],[0,2],[0,0]]],[[[4,0],[6,0],[6,2],[4,2],[4,0]]]]");
        add(cases, "line crosses gap between multipolygon members", geo("$geoWithin", islands),
                geometry("LineString", "[[1,1],[5,1]]"), false);
        add(cases, "disconnected points can occupy separate multipolygon members", geo("$geoWithin", islands),
                geometry("MultiPoint", "[[1,1],[5,1]]"), true);

        String[] operators = {"$box", "$polygon", "$center", "$centerSphere"};
        String[] shapes = {"[[0,0],[2,2]]", "[[0,0],[2,0],[2,2],[0,2]]",
                "[[1,1],1]", "[[1,1],0.01]"};
        for (int i = 0; i < operators.length; i++) {
            add(cases, operators[i] + " matches legacy coordinate pair", legacy(operators[i], shapes[i]),
                    Arrays.asList(1, 1), true);
            add(cases, operators[i] + " rejects outside legacy coordinate pair", legacy(operators[i], shapes[i]),
                    Arrays.asList(20, 20), false);
        }
        // MongoDB 7 accepts this representation too, despite narrower wording in its manual.
        add(cases, "box matches GeoJSON point control", legacy("$box", shapes[0]), inside, true);

        Document elevatedSquare = geometry("Polygon",
                "[[[0,0,100],[10,0,100],[10,10,100],[0,10,100],[0,0,100]]]");
        add(cases, "polygon query includes altitude", geo("$geoWithin", elevatedSquare), inside, true);
        add(cases, "line query includes altitude", geo("$geoIntersects",
                geometry("LineString", "[[0,0,100],[1,0,100]]")), geometry("Point", "[0,0]"), true);
        add(cases, "stored polygon includes altitude", geo("$geoWithin", square),
                geometry("Polygon", "[[[1,1,100],[2,1,100],[1,2,100],[1,1,100]]]"), true);

        Document endpointLine = geometry("LineString", "[[0.7,0],[0.1,0]]");
        add(cases, "exact line endpoint must not throw", geo("$geoIntersects", endpointLine),
                geometry("Point", "[0.1,0]"), true);
        add(cases, "line starting point control", geo("$geoIntersects", endpointLine),
                geometry("Point", "[0.7,0]"), true);
        add(cases, "point away from line control", geo("$geoIntersects", endpointLine),
                geometry("Point", "[0.1,1]"), false);

        for (String operator : new String[]{"$geoWithin", "$geoIntersects"}) {
            Document query = geo(operator, square);
            add(cases, operator + " matches single geometry array", query, Arrays.asList(inside), true);
            add(cases, operator + " matches any geometry in array", query, Arrays.asList(inside, outside), true);
            add(cases, operator + " rejects array of outside geometries", query, Arrays.asList(outside), false);
            add(cases, operator + " negation rejects matching geometry array",
                    new Document("loc", new Document("$not", query.get("loc"))), Arrays.asList(inside), false);
            add(cases, operator + " does not recursively flatten nested geometry arrays", query,
                    Arrays.asList(Arrays.asList(inside)), false);
            add(cases, operator + " respects MultiPoint semantics", query,
                    geometry("MultiPoint", "[[1,1],[20,20]]"), operator.equals("$geoIntersects"));
            add(cases, operator + " respects GeometryCollection semantics", query,
                    new Document("type", "GeometryCollection").append("geometries", Arrays.asList(inside, outside)),
                    operator.equals("$geoIntersects"));
        }

        for (String crs : new String[]{"urn:ogc:def:crs:OGC:1.3:CRS84", "EPSG:4326"}) {
            Document declaredSquare = Document.parse(square.toJson()).append("crs",
                    new Document("type", "name").append("properties", new Document("name", crs)));
            for (String operator : new String[]{"$geoWithin", "$geoIntersects"}) {
                add(cases, operator + " accepts explicit default CRS " + crs, geo(operator, declaredSquare), inside, true);
            }
        }

        for (String operator : new String[]{"$center", "$centerSphere"}) {
            add(cases, operator + " zero radius matches center", legacy(operator, "[[1,1],0]"), inside, true);
            add(cases, operator + " zero radius excludes other points", legacy(operator, "[[1,1],0]"), outside, false);
        }

        add(cases, "zero width box matches point on segment", legacy("$box", "[[0,0],[0,1]]"),
                geometry("Point", "[0,0.5]"), true);
        add(cases, "zero width box excludes point off segment", legacy("$box", "[[0,0],[0,1]]"), inside, false);
        add(cases, "point box matches identical point", legacy("$box", "[[0,0],[0,0]]"),
                geometry("Point", "[0,0]"), true);
        add(cases, "point box excludes another point", legacy("$box", "[[0,0],[0,0]]"), inside, false);

        add(cases, "ring closure treats signed zero as same longitude", geo("$geoWithin",
                geometry("Polygon", "[[[0.0,0.0],[2,0],[0,2],[-0.0,0.0]]]")),
                geometry("Point", "[0.5,0.5]"), true);
        add(cases, "ring closure treats signed zero as same latitude", geo("$geoWithin",
                geometry("Polygon", "[[[0.0,0.0],[2,0],[0,2],[0.0,-0.0]]]")),
                geometry("Point", "[0.5,0.5]"), true);

        String[] objectPositions = {"[{x:0,y:0},{x:2,y:2}]",
                "[{x:0,y:0},{x:2,y:0},{x:2,y:2},{x:0,y:2}]",
                "[{x:1,y:1},1]", "[{x:1,y:1},0.01]"};
        String[] objectShapes = {"{a:[0,0],b:[2,2]}",
                "{a:[0,0],b:[2,0],c:[2,2],d:[0,2]}",
                "{a:[1,1],b:1}", "{a:[1,1],b:0.01}"};
        for (int i = 0; i < operators.length; i++) {
            add(cases, operators[i] + " accepts ordered document coordinates", legacy(operators[i], objectPositions[i]),
                    inside, true);
            add(cases, operators[i] + " accepts ordered document shape", legacy(operators[i], objectShapes[i]), inside, true);
        }
        return cases.stream();
    }

    private static void add(List<MongoQueryCase> cases, String name, Document query, Object location, boolean matches) {
        cases.add(new MongoQueryCase(name, query, new Document("loc", location), matches));
    }

    private static Document geometry(String type, String coordinates) {
        return Document.parse("{type:'" + type + "',coordinates:" + coordinates + "}");
    }

    static Document geo(String operator, Document geometry) {
        return new Document("loc", new Document(operator, new Document("$geometry", geometry)));
    }

    private static Document legacy(String operator, String shape) {
        return Document.parse("{loc:{$geoWithin:{" + operator + ":" + shape + "}}}");
    }

    /** The performance test and live oracle intentionally use exactly the same detailed lines. */
    public static Document detailedLine(int size, double latitude) {
        List<List<Double>> coordinates = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            coordinates.add(Arrays.asList(10.0 * i / size, latitude));
        }
        return new Document("type", "LineString").append("coordinates", coordinates);
    }
}
