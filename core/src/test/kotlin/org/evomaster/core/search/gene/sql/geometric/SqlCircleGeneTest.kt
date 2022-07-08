package org.evomaster.core.search.gene.sql.geometric

import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SqlCircleGeneTest {

    val rand = Randomness().apply { updateSeed(42) }

    @Test
    fun testGetValueAsPrintableString() {
        val gene = SqlCircleGene("circle")
        gene.doInitialize(rand)
        gene.r.value = 1f
        gene.c.x.value=0f
        gene.c.y.value=2f
        assertEquals("\"((0.0, 2.0), 1.0)\"",gene.getValueAsPrintableString())
    }
}