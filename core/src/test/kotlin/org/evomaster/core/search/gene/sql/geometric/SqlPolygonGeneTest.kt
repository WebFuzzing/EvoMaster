package org.evomaster.core.search.gene.sql.geometric

import org.evomaster.core.search.gene.FloatGene
import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SqlPolygonGeneTest {

    @Test
    fun testRandomizePolygonSize3() {
        val gene = SqlPolygonGene(name = "polygon", minLengthOfPolygonRing = 3, onlyNonIntersectingPolygons = true)
        val rand = Randomness()
        gene.randomize(rand, false)
        assertTrue(gene.isValid())
        assertTrue(gene.points.getChildren().size >= 3)
    }

    @Test
    fun testRandomizePolygonSize6() {
        val gene = SqlPolygonGene(name = "polygon", minLengthOfPolygonRing = 6, onlyNonIntersectingPolygons = true)
        val rand = Randomness()
        gene.randomize(rand, false)
        assertTrue(gene.isValid())
        assertTrue(gene.points.getChildren().size >= 6)
    }

    @Test
    fun testInvalidPolygon() {
        val gene = SqlPolygonGene(name = "polygon", minLengthOfPolygonRing = 3, onlyNonIntersectingPolygons = true)
        gene.points.addElement(SqlPointGene("p",
                x = FloatGene("x", value = 0.11125329f),
                y = FloatGene("y", value = 0.8111974f)))
        gene.points.addElement(SqlPointGene("p",
                x = FloatGene("x", value = 0.06739818f),
                y = FloatGene("y", value = 0.44556677f)))
        gene.points.addElement(SqlPointGene("p",
                x = FloatGene("x", value = 0.4146604f),
                y = FloatGene("y", value = 0.23759939f)))
        gene.points.addElement(SqlPointGene("p",
                x = FloatGene("x", value = 0.11125329f),
                y = FloatGene("y", value = 0.8111974f)))
        assertFalse(gene.isValid())

    }

    @Test
    fun testValidPolygon() {
        val gene = SqlPolygonGene(name = "polygon", minLengthOfPolygonRing = 3, onlyNonIntersectingPolygons = true)
        gene.points.addElement(SqlPointGene("p",
                x = FloatGene("x", value = 0f),
                y = FloatGene("y", value = 0f)))
        gene.points.addElement(SqlPointGene("p",
                x = FloatGene("x", value = 1f),
                y = FloatGene("y", value = 0f)))
        gene.points.addElement(SqlPointGene("p",
                x = FloatGene("x", value = 1f),
                y = FloatGene("y", value = 1f)))
        gene.points.addElement(SqlPointGene("p",
                x = FloatGene("x", value = 0f),
                y = FloatGene("y", value = 1f)))
        assertTrue(gene.isValid())
    }

    @Test
    fun testInvalidHourglass() {
        val gene = SqlPolygonGene(name = "polygon", minLengthOfPolygonRing = 3, onlyNonIntersectingPolygons = true)
        gene.points.addElement(SqlPointGene("p",
                x = FloatGene("x", value = 0f),
                y = FloatGene("y", value = 0f)))
        gene.points.addElement(SqlPointGene("p",
                x = FloatGene("x", value = 1f),
                y = FloatGene("y", value = 1f)))
        gene.points.addElement(SqlPointGene("p",
                x = FloatGene("x", value = 1f),
                y = FloatGene("y", value = 0f)))
        gene.points.addElement(SqlPointGene("p",
                x = FloatGene("x", value = 0f),
                y = FloatGene("y", value = 1f)))
        assertFalse(gene.isValid())
    }

}