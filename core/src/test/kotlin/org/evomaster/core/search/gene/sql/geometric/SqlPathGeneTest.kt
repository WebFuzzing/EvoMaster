package org.evomaster.core.search.gene.sql.geometric

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.core.search.gene.FloatGene
import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SqlPathGeneTest {

    val rand = Randomness()

    @Test
    fun testGetValueAsPrintable() {
        val gene = SqlPathGene(name = "path", databaseType = DatabaseType.H2)
        gene.randomize(rand,true)
        gene.points.killAllChildren()
        gene.points.addElement(SqlPointGene("p0",
                x = FloatGene("x", value = 0f),
                y = FloatGene("y", value = 1f)))
        gene.points.addElement(SqlPointGene("p1",
                x = FloatGene("x", value = 1f),
                y = FloatGene("y", value = 1f)))
        gene.points.addElement(SqlPointGene("p2",
                x = FloatGene("x", value = 0f),
                y = FloatGene("y", value = 0f)))
        assertEquals("\"LINESTRING(0.0 1.0, 1.0 1.0, 0.0 0.0)\"",gene.getValueAsPrintableString())
    }
}