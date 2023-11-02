package org.evomaster.client.java.controller.internal.db.sql.mysql;

import org.evomaster.client.java.controller.DatabaseTestTemplate;
import org.evomaster.client.java.sql.SqlScriptRunner;
import org.evomaster.client.java.controller.internal.SutController;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test Geometry Well-Formedness and Validity according to
 * https://dev.mysql.com/doc/refman/8.0/en/geometry-well-formedness-validity.html
 * <p>
 * A geometry is syntactically well-formed if it satisfies conditions such as those in this (nonexhaustive) list:
 * - Linestrings have at least two points
 * - Polygons have at least one ring
 * - Polygon rings are closed (first and last points the same)
 * - Polygon rings have at least 4 points (minimum polygon is a triangle with first and last points the same)
 * - Collections are not empty (except GeometryCollection)
 * <p>
 * A geometry is geometrically valid if it is syntactically well-formed and satisfies conditions such as
 * those in this (nonexhaustive) list:
 * - Polygons are not self-intersecting
 * - Polygon interior rings are inside the exterior ring
 * - Multipolygons do not have overlapping polygons
 */
public class MySQLInsertionTest extends DatabaseMySQLTestInit implements DatabaseTestTemplate {

    @Test
    public void testInsertNegPoint() throws Exception {
        SqlScriptRunner.execCommand(getConnection(), "CREATE TABLE SpatialTable(pointcolumn POINT NOT NULL)");
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO SpatialTable(pointcolumn) VALUES (POINT(-1, -1))");
    }


    @Test
    public void testInsertPoint() throws Exception {
        SqlScriptRunner.execCommand(getConnection(), "CREATE TABLE SpatialTable(pointcolumn POINT NOT NULL)");
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO SpatialTable(pointcolumn) VALUES (POINT(0,0))");
    }

    @Test
    public void testInsertMultipoint() throws Exception {
        SqlScriptRunner.execCommand(getConnection(), "CREATE TABLE SpatialTable(multipointcolumn MULTIPOINT NOT NULL)");
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO SpatialTable(multipointcolumn) VALUES (MULTIPOINT(POINT(0,0),POINT(1,1)))");
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO SpatialTable(multipointcolumn) VALUES (MULTIPOINT(POINT(0,0)))");
        assertThrows(SQLException.class, () ->
                SqlScriptRunner.execCommand(getConnection(), "INSERT INTO SpatialTable(multipointcolumn) VALUES (MULTIPOINT())")
        );
    }

    @Test
    public void testFailInsertOfEmptyMultipoint() throws Exception {
        SqlScriptRunner.execCommand(getConnection(), "CREATE TABLE SpatialTable(multipointcolumn MULTIPOINT NOT NULL)");
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO SpatialTable(multipointcolumn) VALUES (MULTIPOINT(POINT(0,0)))");
    }


    @Test
    public void testInsertLinestring() throws Exception {
        SqlScriptRunner.execCommand(getConnection(), "CREATE TABLE SpatialTable(linestringcolumn LINESTRING NOT NULL)");
        // Linestrings have at least two points
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO SpatialTable(linestringcolumn) VALUES (LINESTRING(POINT(0,0),POINT(1,1)))");
    }

    @Test
    public void testFailInsertLinestringOfOnePoint() throws Exception {
        SqlScriptRunner.execCommand(getConnection(), "CREATE TABLE SpatialTable(linestringcolumn LINESTRING NOT NULL)");
        // expected to fail, Linestrings must have at least two points
        assertThrows(SQLException.class, () ->
            SqlScriptRunner.execCommand(getConnection(), "INSERT INTO SpatialTable(linestringcolumn) VALUES (LINESTRING(POINT(0,0)))")
        );
    }


    @Test
    public void testInsertMultilinestring() throws Exception {
        SqlScriptRunner.execCommand(getConnection(), "CREATE TABLE SpatialTable(multilinestringcolumn MULTILINESTRING NOT NULL)");
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO SpatialTable(multilinestringcolumn) VALUES (MULTILINESTRING(LINESTRING(POINT(0,0),POINT(1,1))))");
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO SpatialTable(multilinestringcolumn) VALUES (MULTILINESTRING(LINESTRING(POINT(0,0),POINT(1,1)), LINESTRING(POINT(0,0),POINT(1,1))))");
    }

    @Test
    public void testInsertEmptyMultilinestring() throws Exception {
        SqlScriptRunner.execCommand(getConnection(), "CREATE TABLE SpatialTable(multilinestringcolumn MULTILINESTRING NOT NULL)");
        assertThrows(SQLException.class, () -> SqlScriptRunner.execCommand(getConnection(), "INSERT INTO SpatialTable(multilinestringcolumn) VALUES (MULTILINESTRING())"));
    }


    @Test
    public void testInsertPolygon() throws Exception {
        SqlScriptRunner.execCommand(getConnection(), "CREATE TABLE SpatialTable(polygoncolumn POLYGON NOT NULL)");
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO SpatialTable(polygoncolumn) VALUES (POLYGON( LINESTRING(POINT(0,0),POINT(0,0),POINT(1,1),POINT(0,0)) ))");
    }

    @Test
    public void testInsertAllEqualPolygon() throws Exception {
        SqlScriptRunner.execCommand(getConnection(), "CREATE TABLE SpatialTable(polygoncolumn POLYGON NOT NULL)");
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO SpatialTable(polygoncolumn) VALUES (POLYGON( LINESTRING(POINT(0,0),POINT(0,0),POINT(0,0),POINT(0,0)) ))");
    }


    @Test
    public void testInsertGeometryCollectionOfPoint() throws Exception {
        SqlScriptRunner.execCommand(getConnection(), "CREATE TABLE SpatialTable(geometrycollectioncolumn GEOMETRYCOLLECTION NOT NULL)");
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO SpatialTable(geometrycollectioncolumn) VALUES (GEOMETRYCOLLECTION(POINT(0,0)) )");
    }

    @Test
    public void testInsertGeometryCollectionOfPointAndLinestring() throws Exception {
        SqlScriptRunner.execCommand(getConnection(), "CREATE TABLE SpatialTable(geometrycollectioncolumn GEOMETRYCOLLECTION NOT NULL)");
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO SpatialTable(geometrycollectioncolumn) VALUES (GEOMETRYCOLLECTION(POINT(0,0), LINESTRING(POINT(0,0),POINT(1,1))) )");
    }

    @Test
    public void testInsertPolygonHourglass() throws Exception {
        SqlScriptRunner.execCommand(getConnection(), "CREATE TABLE SpatialTable(polygoncolumn POLYGON NOT NULL)");
        assertThrows(SQLException.class, () -> SqlScriptRunner.execCommand(getConnection(), "INSERT INTO SpatialTable(polygoncolumn) VALUES (ST_GeomFromText('POLYGON(POINT(0,0) , POINT(1,1) , POINT(0,1) , POINT(1,0) , POINT(0,0) )'))"));
    }

    @Test
    public void testInsertPolygonBox() throws Exception {
        SqlScriptRunner.execCommand(getConnection(), "CREATE TABLE SpatialTable(polygoncolumn POLYGON NOT NULL)");
        SqlScriptRunner.execCommand(getConnection(), "INSERT INTO SpatialTable(polygoncolumn) VALUES (ST_GeomFromText('Polygon((0 0,0 3,3 3,3 0,0 0))'))");

    }


    @Override
    public Connection getConnection() {
        return connection;
    }

    @Override
    public SutController getSutController() {
        return new DatabaseFakeMySQLSutController(connection);
    }



}
