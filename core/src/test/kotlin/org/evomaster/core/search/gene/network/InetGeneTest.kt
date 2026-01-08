package org.evomaster.core.search.gene.network

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class InetGeneTest {

    @Test
    fun testSetValueBasedOn() {
        val gene = InetGene("inet")

        assertEquals("0.0.0.0", gene.getValueAsRawString())
        assertTrue(gene.unsafeSetFromStringValue("127.0.0.1"))
        assertEquals("127.0.0.1", gene.getValueAsRawString())
    }

    @Test
    fun testInvalidAddressForSetValueBasedOn() {
        val gene = InetGene("inet")

        assertEquals("0.0.0.0", gene.getValueAsRawString())

        assertFalse(gene.unsafeSetFromStringValue("a.0.0.e"))
        assertFalse(gene.unsafeSetFromStringValue("localhost"))
    }
}
