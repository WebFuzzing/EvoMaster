package org.evomaster.core.search.gene.sql.geometric

import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SqlLineGeneTest {

    val rand = Randomness().apply { updateSeed(42) }

    @Test
    fun testGetValueAsPrintableString() {
        val gene = SqlLineGene("line")
        gene.doInitialize(rand)
        gene.p.x.value=0f
        gene.p.y.value=1f
        gene.q.x.value=2f
        gene.q.y.value=3f
        assertEquals("\"((0.0, 1.0), (2.0, 3.0))\"",gene.getValueAsPrintableString())
    }
}