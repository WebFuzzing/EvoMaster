package org.evomaster.core.sql.schema.h2

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class H2GeometryTypeParserTest {

    @Test
    fun testValidGeometryPoint() {
        val expr = "GEOMETRY(POINT)"
        val parser = H2GeometryTypeParser()
        assertTrue(parser.isParsable(expr))
        val h2GeometryType = parser.parse(expr)
        assertEquals("POINT", h2GeometryType.geometricObjectString)

    }

    @Test
    fun testInvalid() {
        val expr = "GEOMETRY(PONT)"
        val parser = H2GeometryTypeParser()
        assertFalse(parser.isParsable(expr))
        assertThrows<IllegalArgumentException> { parser.parse(expr) }
    }

    @Test
    fun testValidGeometryPointBlankSpaces() {
        val expr = "  GEOMETRY  (  POINT  )  "
        val parser = H2GeometryTypeParser()
        assertTrue(parser.isParsable(expr))
        val h2GeometryType = parser.parse(expr)
        assertEquals("POINT", h2GeometryType.geometricObjectString)
    }

    @Test
    fun testValidGeometrySri() {
        val expr = "GEOMETRY(POINT,100)"
        val parser = H2GeometryTypeParser()
        assertTrue(parser.isParsable(expr))
        val h2GeometryType = parser.parse(expr)
        assertEquals("POINT", h2GeometryType.geometricObjectString)
        assertEquals(100, h2GeometryType.spatialReferenceSystemIdentifierInt)
    }

    @Test
    fun testValidGeometryGeometry() {
        val expr = "GEOMETRY(GEOMETRY)"
        val parser = H2GeometryTypeParser()
        assertTrue(parser.isParsable(expr))
        val h2GeometryType = parser.parse(expr)
        assertEquals("GEOMETRY", h2GeometryType.geometricObjectString)
    }

    @Test
    fun testValidLinestring() {
        val expr = "GEOMETRY(LINESTRING)"
        val parser = H2GeometryTypeParser()
        assertTrue(parser.isParsable(expr))
        val h2GeometryType = parser.parse(expr)
        assertEquals("LINESTRING",h2GeometryType.geometricObjectString)
    }

    @Test
    fun testValidPolygon() {
        val expr = "GEOMETRY(POLYGON)"
        val parser = H2GeometryTypeParser()
        assertTrue(parser.isParsable(expr))
        val h2GeometryType = parser.parse(expr)
        assertEquals("POLYGON", h2GeometryType.geometricObjectString)
    }

    @Test
    fun testValidMultipoint() {
        val expr = "GEOMETRY(MULTIPOINT)"
        val parser = H2GeometryTypeParser()
        assertTrue(parser.isParsable(expr))
        val h2GeometryType = parser.parse(expr)
        assertEquals("MULTIPOINT", h2GeometryType.geometricObjectString)
    }

    @Test
    fun testValidMultilinestring() {
        val expr = "GEOMETRY(MULTILINESTRING)"
        val parser = H2GeometryTypeParser()
        assertTrue(parser.isParsable(expr))
        val h2GeometryType = parser.parse(expr)
        assertEquals("MULTILINESTRING", h2GeometryType.geometricObjectString)
    }

    @Test
    fun testValidMultipolygon() {
        val expr = "GEOMETRY(MULTIPOLYGON)"
        val parser = H2GeometryTypeParser()
        assertTrue(parser.isParsable(expr))
        val h2GeometryType = parser.parse(expr)
        assertEquals("MULTIPOLYGON", h2GeometryType.geometricObjectString)
    }

    @Test
    fun testValidGeometryCollection() {
        val expr = "GEOMETRY(GEOMETRYCOLLECTION)"
        val parser = H2GeometryTypeParser()
        assertTrue(parser.isParsable(expr))
        val h2GeometryType = parser.parse(expr)
        assertEquals("GEOMETRYCOLLECTION", h2GeometryType.geometricObjectString)
    }

    @Test
    fun testValidPointDimension() {
        val expr = "GEOMETRY(POINT ZM)"
        val parser = H2GeometryTypeParser()
        assertTrue(parser.isParsable(expr))
        val h2GeometryType = parser.parse(expr)
        assertEquals("POINT", h2GeometryType.geometricObjectString)
        assertEquals("ZM", h2GeometryType.geometricDimensionString)
    }

    @Test
    fun testValidPointDimensionSri() {
        val expr = "GEOMETRY(POINT ZM, 100)"
        val parser = H2GeometryTypeParser()
        assertTrue(parser.isParsable(expr))
        val h2GeometryType = parser.parse(expr)
        assertEquals("POINT", h2GeometryType.geometricObjectString)
        assertEquals("ZM", h2GeometryType.geometricDimensionString)
        assertEquals(100, h2GeometryType.spatialReferenceSystemIdentifierInt)
    }

    @Test
    fun testValidPointSri() {
        val expr = "GEOMETRY(POINT, 12)"
        val parser = H2GeometryTypeParser()
        assertTrue(parser.isParsable(expr))
        val h2GeometryType = parser.parse(expr)
        assertEquals("POINT", h2GeometryType.geometricObjectString)
        assertEquals(12, h2GeometryType.spatialReferenceSystemIdentifierInt)
    }


}