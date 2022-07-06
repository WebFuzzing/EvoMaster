package org.evomaster.core.search.gene.sql.geometric

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SqlPointGeneTest {

    val rand = Randomness()

    @Test
    fun testGetValueForNontEmpty() {
        val gene =SqlPointGene("p0", databaseType = DatabaseType.H2)
        gene.randomize(rand,true)
        gene.x.value = 0f
        gene.y.value = 0f
        assertEquals("\"POINT(0.0 0.0)\"", gene.getValueAsPrintableString())
    }


}