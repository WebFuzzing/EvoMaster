package org.evomaster.core.search.gene.uri

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UrlHttpGeneTest {

    @Test
    fun testSetValueBasedOn() {
        val gene = UrlHttpGene("host")

        assertEquals("0.0.0.0", gene.host.getValueAsRawString())

        assertTrue(gene.unsafeSetFromStringValue("https://localhost:8080/"))

        assertEquals("8080", gene.port.getValueAsRawString())
        assertEquals("localhost", gene.host.getValueAsRawString())
    }
}
