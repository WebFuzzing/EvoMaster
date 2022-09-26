package org.evomaster.core.search.gene.sql.geometric

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.core.search.gene.numeric.FloatGene
import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SqlMultiPointGeneTest {

    val rand = Randomness()

    @BeforeEach
    fun resetSeed() {
        rand.updateSeed(42)
    }

    @Test
    fun testGetValueForEmptyH2() {
        val gene = SqlMultiPointGene("multipoint", databaseType = DatabaseType.H2)
        gene.randomize(rand, true)
        gene.points.killAllChildren()

        assertEquals("\"MULTIPOINT EMPTY\"", gene.getValueAsPrintableString())
    }

    @Test
    fun testGetValueForNontEmptyH2() {
        val gene = SqlMultiPointGene("multipoint", databaseType = DatabaseType.H2)
        gene.randomize(rand, true)
        gene.points.killAllChildren()

        gene.points.addElement(
                SqlPointGene("p0",databaseType = DatabaseType.H2,
                        x = FloatGene("x", value = 0f),
                        y = FloatGene("y", value = 0f)
                )
        )
        assertEquals("\"MULTIPOINT(0.0 0.0)\"", gene.getValueAsPrintableString())
    }

    @Test
    fun testGetValueForTwoPointsH2() {
        val gene = SqlMultiPointGene("multipoint", databaseType = DatabaseType.H2)
        gene.randomize(rand, true)
        gene.points.killAllChildren()

        gene.points.addElement(
                SqlPointGene("p0", databaseType = DatabaseType.H2,
                        x = FloatGene("x", value = 0f),
                        y = FloatGene("y", value = 0f)
                )
        )
        gene.points.addElement(
                SqlPointGene("p1", databaseType = DatabaseType.H2,
                        x = FloatGene("x", value = 1.0f),
                        y = FloatGene("y", value = 1.0f)
                )
        )
        assertEquals("\"MULTIPOINT(0.0 0.0, 1.0 1.0)\"", gene.getValueAsPrintableString())
    }

    @Test
    fun testGetValueForNontEmptyMySQL() {
        val gene = SqlMultiPointGene("multipoint", databaseType = DatabaseType.MYSQL)
        gene.randomize(rand, true)
        gene.points.killAllChildren()

        gene.points.addElement(
                SqlPointGene("p0", databaseType = DatabaseType.MYSQL,
                        x = FloatGene("x", value = 0f),
                        y = FloatGene("y", value = 0f)
                )
        )
        assertEquals("MULTIPOINT(POINT(0.0, 0.0))", gene.getValueAsPrintableString())
    }

    @Test
    fun testGetValueForTwoPointsMySQL() {
        val gene = SqlMultiPointGene("multipoint", databaseType = DatabaseType.MYSQL)
        gene.randomize(rand, true)
        gene.points.killAllChildren()

        gene.points.addElement(
                SqlPointGene("p0", databaseType = DatabaseType.MYSQL,
                        x = FloatGene("x", value = 0f),
                        y = FloatGene("y", value = 0f)
                )
        )
        gene.points.addElement(
                SqlPointGene("p1", databaseType = DatabaseType.MYSQL,
                        x = FloatGene("x", value = 1.0f),
                        y = FloatGene("y", value = 1.0f)
                )
        )
        assertEquals("MULTIPOINT(POINT(0.0, 0.0), POINT(1.0, 1.0))", gene.getValueAsPrintableString())
    }


}