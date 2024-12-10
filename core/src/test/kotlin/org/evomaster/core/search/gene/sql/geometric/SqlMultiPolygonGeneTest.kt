package org.evomaster.core.search.gene.sql.geometric

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.core.search.gene.numeric.FloatGene
import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SqlMultiPolygonGeneTest {

    val rand = Randomness().apply { updateSeed(42) }

    @Test
    fun testGetValueAsPrintableStringH2Empty() {
        val polygonGene = SqlPolygonGene(name = "polygon", databaseType = DatabaseType.H2, minLengthOfPolygonRing = 2, onlyNonIntersectingPolygons = true)
        polygonGene.randomize(rand,true)
        polygonGene.points.killAllChildren()
        polygonGene.points.addElement(SqlPointGene("p",
                x = FloatGene("x", value = 0f),
                y = FloatGene("y", value = 1f)
        ))
        polygonGene.points.addElement(SqlPointGene("p",
                x = FloatGene("x", value = 1f),
                y = FloatGene("y", value = 1f)
        ))
        polygonGene.points.addElement(SqlPointGene("p",
                x = FloatGene("x", value = 0f),
                y = FloatGene("y", value = 0f)
        ))

        val gene = SqlMultiPolygonGene(name = "multipolygon" , databaseType = DatabaseType.H2)
        gene.randomize(rand,true)
        gene.polygons.killAllChildren()
        assertEquals("\"MULTIPOLYGON EMPTY\"", gene.getValueAsPrintableString())

        gene.polygons.addElement(polygonGene)
        assertEquals("\"MULTIPOLYGON(((0.0 1.0, 1.0 1.0, 0.0 0.0, 0.0 1.0)))\"", gene.getValueAsPrintableString())
    }

    @Test
    fun testGetValueAsPrintableStringH2() {
        val polygonGene0 = SqlPolygonGene(name = "polygon", databaseType = DatabaseType.H2, minLengthOfPolygonRing = 2, onlyNonIntersectingPolygons = true)
        polygonGene0.randomize(rand,true)
        polygonGene0.points.killAllChildren()
        polygonGene0.points.addElement(SqlPointGene("p",databaseType = DatabaseType.H2,
                x = FloatGene("x", value = 0f),
                y = FloatGene("y", value = 1f)
        ))
        polygonGene0.points.addElement(SqlPointGene("p",databaseType = DatabaseType.H2,
                x = FloatGene("x", value = 1f),
                y = FloatGene("y", value = 1f)
        ))
        polygonGene0.points.addElement(SqlPointGene("p",databaseType = DatabaseType.H2,
                x = FloatGene("x", value = 0f),
                y = FloatGene("y", value = 0f)
        ))

        val polygonGene1 = SqlPolygonGene(name = "polygon", databaseType = DatabaseType.H2, minLengthOfPolygonRing = 2, onlyNonIntersectingPolygons = true)
        polygonGene1.randomize(rand,true)
        polygonGene1.points.killAllChildren()
        polygonGene1.points.addElement(SqlPointGene("p",databaseType = DatabaseType.H2,
                x = FloatGene("x", value = 0f),
                y = FloatGene("y", value = 0f)
        ))
        polygonGene1.points.addElement(SqlPointGene("p",databaseType = DatabaseType.H2,
                x = FloatGene("x", value = 1f),
                y = FloatGene("y", value = 1f)
        ))
        polygonGene1.points.addElement(SqlPointGene("p",databaseType = DatabaseType.H2,
                x = FloatGene("x", value = 1f),
                y = FloatGene("y", value = 0f)
        ))

        val gene = SqlMultiPolygonGene(name = "multipolygon" , databaseType = DatabaseType.H2)
        gene.randomize(rand,true)
        gene.polygons.killAllChildren()

        gene.polygons.addElement(polygonGene0)
        gene.polygons.addElement(polygonGene1)

        assertEquals("\"MULTIPOLYGON(((0.0 1.0, 1.0 1.0, 0.0 0.0, 0.0 1.0)), ((0.0 0.0, 1.0 1.0, 1.0 0.0, 0.0 0.0)))\"", gene.getValueAsPrintableString())
    }


    @Test
    fun testGetValueAsPrintableStringMySQL() {
        val polygonGene0 = SqlPolygonGene(name = "polygon", databaseType = DatabaseType.MYSQL, minLengthOfPolygonRing = 2, onlyNonIntersectingPolygons = true)
        polygonGene0.randomize(rand,true)
        polygonGene0.points.killAllChildren()
        polygonGene0.points.addElement(SqlPointGene("p",databaseType = DatabaseType.MYSQL,
                x = FloatGene("x", value = 0f),
                y = FloatGene("y", value = 1f)
        ))
        polygonGene0.points.addElement(SqlPointGene("p",databaseType = DatabaseType.MYSQL,
                x = FloatGene("x", value = 1f),
                y = FloatGene("y", value = 1f)
        ))
        polygonGene0.points.addElement(SqlPointGene("p",databaseType = DatabaseType.MYSQL,
                x = FloatGene("x", value = 0f),
                y = FloatGene("y", value = 0f)
        ))

        val polygonGene1 = SqlPolygonGene(name = "polygon", databaseType = DatabaseType.MYSQL, minLengthOfPolygonRing = 2, onlyNonIntersectingPolygons = true)
        polygonGene1.randomize(rand,true)
        polygonGene1.points.killAllChildren()
        polygonGene1.points.addElement(SqlPointGene("p",databaseType = DatabaseType.MYSQL,
                x = FloatGene("x", value = 0f),
                y = FloatGene("y", value = 0f)
        ))
        polygonGene1.points.addElement(SqlPointGene("p",databaseType = DatabaseType.MYSQL,
                x = FloatGene("x", value = 1f),
                y = FloatGene("y", value = 1f)
        ))
        polygonGene1.points.addElement(SqlPointGene("p",databaseType = DatabaseType.MYSQL,
                x = FloatGene("x", value = 1f),
                y = FloatGene("y", value = 0f)
        ))

        val gene = SqlMultiPolygonGene(name = "multipolygon" , databaseType = DatabaseType.MYSQL)
        gene.randomize(rand,true)
        gene.polygons.killAllChildren()

        gene.polygons.addElement(polygonGene0)
        gene.polygons.addElement(polygonGene1)

        assertEquals("MULTIPOLYGON(POLYGON(LINESTRING(POINT(0.0, 1.0), POINT(1.0, 1.0), POINT(0.0, 0.0), POINT(0.0, 1.0))), POLYGON(LINESTRING(POINT(0.0, 0.0), POINT(1.0, 1.0), POINT(1.0, 0.0), POINT(0.0, 0.0))))", gene.getValueAsPrintableString())
    }

}