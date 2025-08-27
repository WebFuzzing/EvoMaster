package org.evomaster.core.search.gene.network

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class InetGeneTest {

    @Test
    fun testSetValueBasedOn() {
        val gene = InetGene("inet")

        assertEquals("0.0.0.0", gene.getValueAsRawString())
        assertTrue(gene.setValueBasedOn("4.3.2.1"))
        assertEquals("4.3.2.1", gene.getValueAsRawString())
    }
}
