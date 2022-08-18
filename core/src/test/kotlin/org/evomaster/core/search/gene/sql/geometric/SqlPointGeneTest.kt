package org.evomaster.core.search.gene.sql.geometric

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SqlPointGeneTest {

    val rand = Randomness()

    @BeforeEach
    fun resetSeed() {
        rand.updateSeed(42)
    }
    @Test
    fun testGetValueH2() {
        val gene =SqlPointGene("p0", databaseType = DatabaseType.H2)
        gene.randomize(rand,true)
        gene.x.value = 0f
        gene.y.value = 0f
        assertEquals("\"POINT(0.0 0.0)\"", gene.getValueAsPrintableString())
    }

    @Test
    fun testGetValueMySQL() {
        val gene =SqlPointGene("p0", databaseType = DatabaseType.MYSQL)
        gene.randomize(rand,true)
        gene.x.value = 0f
        gene.y.value = 0f
        assertEquals("POINT(0.0, 0.0)", gene.getValueAsPrintableString())
    }

    @Test
    fun testGetValuePostgre() {
        val gene =SqlPointGene("p0", databaseType = DatabaseType.POSTGRES)
        gene.randomize(rand,true)
        gene.x.value = 0f
        gene.y.value = 0f
        assertEquals("\"(0.0, 0.0)\"", gene.getValueAsPrintableString())
    }

}