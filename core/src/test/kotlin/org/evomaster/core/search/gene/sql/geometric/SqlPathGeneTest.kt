package org.evomaster.core.search.gene.sql.geometric

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.core.search.gene.numeric.FloatGene
import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SqlPathGeneTest {

    val rand = Randomness()

    @BeforeEach
    fun resetSeed() {
        rand.updateSeed(42)
    }

    @Test
    fun testGetValueAsPrintableH2() {
        val gene = SqlPathGene(name = "path", databaseType = DatabaseType.H2)
        gene.randomize(rand,true)
        gene.points.killAllChildren()
        gene.points.addElement(SqlPointGene("p0",databaseType = DatabaseType.H2,
                x = FloatGene("x", value = 0f),
                y = FloatGene("y", value = 1f)
        ))
        gene.points.addElement(SqlPointGene("p1",databaseType = DatabaseType.H2,
                x = FloatGene("x", value = 1f),
                y = FloatGene("y", value = 1f)
        ))
        gene.points.addElement(SqlPointGene("p2", databaseType = DatabaseType.H2,
                x = FloatGene("x", value = 0f),
                y = FloatGene("y", value = 0f)
        ))
        assertEquals("\"LINESTRING(0.0 1.0, 1.0 1.0, 0.0 0.0)\"",gene.getValueAsPrintableString())
    }

    @Test
    fun testGetValueAsPrintableMYSQL() {
        val gene = SqlPathGene(name = "path", databaseType = DatabaseType.MYSQL)
        gene.randomize(rand,true)
        gene.points.killAllChildren()
        gene.points.addElement(SqlPointGene("p0",databaseType = DatabaseType.MYSQL,
                x = FloatGene("x", value = 0f),
                y = FloatGene("y", value = 1f)
        ))
        gene.points.addElement(SqlPointGene("p1",databaseType = DatabaseType.MYSQL,
                x = FloatGene("x", value = 1f),
                y = FloatGene("y", value = 1f)
        ))
        gene.points.addElement(SqlPointGene("p2", databaseType = DatabaseType.MYSQL,
                x = FloatGene("x", value = 0f),
                y = FloatGene("y", value = 0f)
        ))
        assertEquals("LINESTRING(POINT(0.0, 1.0), POINT(1.0, 1.0), POINT(0.0, 0.0))",gene.getValueAsPrintableString())
    }

    @Test
    fun testGetValueAsPrintablePostgres() {
        val gene = SqlPathGene(name = "path", databaseType = DatabaseType.POSTGRES)
        gene.randomize(rand,true)
        gene.points.killAllChildren()
        gene.points.addElement(SqlPointGene("p0",databaseType = DatabaseType.POSTGRES,
                x = FloatGene("x", value = 0f),
                y = FloatGene("y", value = 1f)
        ))
        gene.points.addElement(SqlPointGene("p1",databaseType = DatabaseType.POSTGRES,
                x = FloatGene("x", value = 1f),
                y = FloatGene("y", value = 1f)
        ))
        gene.points.addElement(SqlPointGene("p2", databaseType = DatabaseType.POSTGRES,
                x = FloatGene("x", value = 0f),
                y = FloatGene("y", value = 0f)
        ))
        assertEquals("\"((0.0, 1.0), (1.0, 1.0), (0.0, 0.0))\"",gene.getValueAsPrintableString())
    }

}