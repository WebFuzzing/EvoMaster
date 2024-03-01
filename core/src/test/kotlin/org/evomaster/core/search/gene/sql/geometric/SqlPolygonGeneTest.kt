package org.evomaster.core.search.gene.sql.geometric

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.core.search.gene.numeric.FloatGene
import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SqlPolygonGeneTest {

    val rand = Randomness()

    @BeforeEach
    fun resetSeed() {
        rand.updateSeed(42)
    }

    @Test
    fun testRandomizePolygonSize3() {
        val gene = SqlPolygonGene(name = "polygon", minLengthOfPolygonRing = 3, onlyNonIntersectingPolygons = true)
        gene.randomize(rand, false)
        assertTrue(gene.isLocallyValid())
        assertTrue(gene.points.getViewOfChildren().size >= 3)
    }

    @Test
    fun testRandomizePolygonSize6() {
        val gene = SqlPolygonGene(name = "polygon", minLengthOfPolygonRing = 6, onlyNonIntersectingPolygons = true)
        gene.randomize(rand, false)
        assertTrue(gene.isLocallyValid())
        assertTrue(gene.points.getViewOfChildren().size >= 6)
    }

    @Test
    fun testInvalidPolygon() {
        val gene = SqlPolygonGene(name = "polygon", minLengthOfPolygonRing = 3, onlyNonIntersectingPolygons = true)
        gene.points.addElement(
            SqlPointGene(
                "p",
                x = FloatGene("x", value = 0.11125329f),
                y = FloatGene("y", value = 0.8111974f)
            )
        )
        gene.points.addElement(
            SqlPointGene(
                "p",
                x = FloatGene("x", value = 0.06739818f),
                y = FloatGene("y", value = 0.44556677f)
            )
        )
        gene.points.addElement(
            SqlPointGene(
                "p",
                x = FloatGene("x", value = 0.4146604f),
                y = FloatGene("y", value = 0.23759939f)
            )
        )
        gene.points.addElement(
            SqlPointGene(
                "p",
                x = FloatGene("x", value = 0.11125329f),
                y = FloatGene("y", value = 0.8111974f)
            )
        )
        assertFalse(gene.isLocallyValid())

    }

    @Test
    fun testValidPolygon() {
        val gene = SqlPolygonGene(name = "polygon", minLengthOfPolygonRing = 3, onlyNonIntersectingPolygons = true)
        gene.points.addElement(
            SqlPointGene(
                "p",
                x = FloatGene("x", value = 0f),
                y = FloatGene("y", value = 0f)
            )
        )
        gene.points.addElement(
            SqlPointGene(
                "p",
                x = FloatGene("x", value = 1f),
                y = FloatGene("y", value = 0f)
            )
        )
        gene.points.addElement(
            SqlPointGene(
                "p",
                x = FloatGene("x", value = 1f),
                y = FloatGene("y", value = 1f)
            )
        )
        gene.points.addElement(
            SqlPointGene(
                "p",
                x = FloatGene("x", value = 0f),
                y = FloatGene("y", value = 1f)
            )
        )
        assertTrue(gene.isLocallyValid())
    }

    @Test
    fun testInvalidHourglass() {
        val gene = SqlPolygonGene(name = "polygon", minLengthOfPolygonRing = 3, onlyNonIntersectingPolygons = true)
        gene.points.addElement(
            SqlPointGene(
                "p",
                x = FloatGene("x", value = 0f),
                y = FloatGene("y", value = 0f)
            )
        )
        gene.points.addElement(
            SqlPointGene(
                "p",
                x = FloatGene("x", value = 1f),
                y = FloatGene("y", value = 1f)
            )
        )
        gene.points.addElement(
            SqlPointGene(
                "p",
                x = FloatGene("x", value = 1f),
                y = FloatGene("y", value = 0f)
            )
        )
        gene.points.addElement(
            SqlPointGene(
                "p",
                x = FloatGene("x", value = 0f),
                y = FloatGene("y", value = 1f)
            )
        )
        assertFalse(gene.isLocallyValid())
    }

    @Test
    fun testGetValueAsPrintableH2() {
        val gene = SqlPolygonGene(
            name = "polygon",
            databaseType = DatabaseType.H2,
            minLengthOfPolygonRing = 2,
            onlyNonIntersectingPolygons = true
        )
        gene.randomize(rand, true)
        gene.points.killAllChildren()
        gene.points.addElement(
            SqlPointGene(
                "p", databaseType = DatabaseType.H2,
                x = FloatGene("x", value = 0f),
                y = FloatGene("y", value = 1f)
            )
        )
        gene.points.addElement(
            SqlPointGene(
                "p", databaseType = DatabaseType.H2,
                x = FloatGene("x", value = 1f),
                y = FloatGene("y", value = 1f)
            )
        )
        gene.points.addElement(
            SqlPointGene(
                "p", databaseType = DatabaseType.H2,
                x = FloatGene("x", value = 0f),
                y = FloatGene("y", value = 0f)
            )
        )
        assertEquals("\"POLYGON((0.0 1.0, 1.0 1.0, 0.0 0.0, 0.0 1.0))\"", gene.getValueAsPrintableString())
    }

    @Test
    fun testGetValueAsPrintableMySQL() {
        val gene = SqlPolygonGene(
            name = "polygon",
            databaseType = DatabaseType.MYSQL,
            minLengthOfPolygonRing = 2,
            onlyNonIntersectingPolygons = true
        )
        gene.randomize(rand, true)
        gene.points.killAllChildren()
        gene.points.addElement(
            SqlPointGene(
                "p", databaseType = DatabaseType.MYSQL,
                x = FloatGene("x", value = 0f),
                y = FloatGene("y", value = 1f)
            )
        )
        gene.points.addElement(
            SqlPointGene(
                "p", databaseType = DatabaseType.MYSQL,
                x = FloatGene("x", value = 1f),
                y = FloatGene("y", value = 1f)
            )
        )
        gene.points.addElement(
            SqlPointGene(
                "p", databaseType = DatabaseType.MYSQL,
                x = FloatGene("x", value = 0f),
                y = FloatGene("y", value = 0f)
            )
        )
        assertEquals(
            "POLYGON(LINESTRING(POINT(0.0, 1.0), POINT(1.0, 1.0), POINT(0.0, 0.0), POINT(0.0, 1.0)))",
            gene.getValueAsPrintableString()
        )
    }

    @Test
    fun testGetValueAsPrintablePostgres() {
        val gene = SqlPolygonGene(
            name = "polygon",
            databaseType = DatabaseType.POSTGRES,
            minLengthOfPolygonRing = 2,
            onlyNonIntersectingPolygons = true
        )
        gene.randomize(rand, true)
        gene.points.killAllChildren()
        gene.points.addElement(
            SqlPointGene(
                "p", databaseType = DatabaseType.POSTGRES,
                x = FloatGene("x", value = 0f),
                y = FloatGene("y", value = 1f)
            )
        )
        gene.points.addElement(
            SqlPointGene(
                "p", databaseType = DatabaseType.POSTGRES,
                x = FloatGene("x", value = 1f),
                y = FloatGene("y", value = 1f)
            )
        )
        gene.points.addElement(
            SqlPointGene(
                "p", databaseType = DatabaseType.POSTGRES,
                x = FloatGene("x", value = 0f),
                y = FloatGene("y", value = 0f)
            )
        )
        assertEquals("\"((0.0, 1.0), (1.0, 1.0), (0.0, 0.0))\"", gene.getValueAsPrintableString())
    }
}