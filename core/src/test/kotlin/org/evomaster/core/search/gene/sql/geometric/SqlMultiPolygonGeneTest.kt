package org.evomaster.core.search.gene.sql.geometric

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.core.search.gene.FloatGene
import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SqlMultiPolygonGeneTest {

    val rand = Randomness()

    @Test
    fun testGetValueAsPrintableString() {
        val polygonGene = SqlPolygonGene(name = "polygon", databaseType = DatabaseType.H2, minLengthOfPolygonRing = 2, onlyNonIntersectingPolygons = true)
        polygonGene.randomize(rand,true)
        polygonGene.points.killAllChildren()
        polygonGene.points.addElement(SqlPointGene("p",
                x = FloatGene("x", value = 0f),
                y = FloatGene("y", value = 1f)))
        polygonGene.points.addElement(SqlPointGene("p",
                x = FloatGene("x", value = 1f),
                y = FloatGene("y", value = 1f)))
        polygonGene.points.addElement(SqlPointGene("p",
                x = FloatGene("x", value = 0f),
                y = FloatGene("y", value = 0f)))

        val gene = SqlMultiPolygonGene(name = "multipolygon" , databaseType = DatabaseType.H2)
        gene.randomize(rand,true)
        gene.polygons.killAllChildren()
        assertEquals("\"MULTIPOLYGON EMPTY\"", gene.getValueAsPrintableString())

        gene.polygons.addElement(polygonGene)
        assertEquals("\"MULTIPOLYGON(((0.0 1.0, 1.0 1.0, 0.0 0.0, 0.0 1.0)))\"", gene.getValueAsPrintableString())
    }

    @Test
    fun testTwoPolygonsGetValueAsPrintableString() {
        val polygonGene0 = SqlPolygonGene(name = "polygon", databaseType = DatabaseType.H2, minLengthOfPolygonRing = 2, onlyNonIntersectingPolygons = true)
        polygonGene0.randomize(rand,true)
        polygonGene0.points.killAllChildren()
        polygonGene0.points.addElement(SqlPointGene("p",
                x = FloatGene("x", value = 0f),
                y = FloatGene("y", value = 1f)))
        polygonGene0.points.addElement(SqlPointGene("p",
                x = FloatGene("x", value = 1f),
                y = FloatGene("y", value = 1f)))
        polygonGene0.points.addElement(SqlPointGene("p",
                x = FloatGene("x", value = 0f),
                y = FloatGene("y", value = 0f)))

        val polygonGene1 = SqlPolygonGene(name = "polygon", databaseType = DatabaseType.H2, minLengthOfPolygonRing = 2, onlyNonIntersectingPolygons = true)
        polygonGene1.randomize(rand,true)
        polygonGene1.points.killAllChildren()
        polygonGene1.points.addElement(SqlPointGene("p",
                x = FloatGene("x", value = 0f),
                y = FloatGene("y", value = 0f)))
        polygonGene1.points.addElement(SqlPointGene("p",
                x = FloatGene("x", value = 1f),
                y = FloatGene("y", value = 1f)))
        polygonGene1.points.addElement(SqlPointGene("p",
                x = FloatGene("x", value = 1f),
                y = FloatGene("y", value = 0f)))

        val gene = SqlMultiPolygonGene(name = "multipolygon" , databaseType = DatabaseType.H2)
        gene.randomize(rand,true)
        gene.polygons.killAllChildren()

        gene.polygons.addElement(polygonGene0)
        gene.polygons.addElement(polygonGene1)

        assertEquals("\"MULTIPOLYGON(((0.0 1.0, 1.0 1.0, 0.0 0.0, 0.0 1.0)), ((0.0 0.0, 1.0 1.0, 1.0 0.0, 0.0 0.0)))\"", gene.getValueAsPrintableString())
    }

}