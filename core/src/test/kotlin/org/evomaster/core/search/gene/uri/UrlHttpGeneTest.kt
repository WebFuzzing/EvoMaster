package org.evomaster.core.search.gene.uri

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UrlHttpGeneTest {

    @Test
    fun testSetValueBasedOn() {
        val gene = UrlHttpGene("host")

        assertEquals("0.0.0.0", gene.host.getValueAsRawString())

        assertTrue(gene.setValueBasedOn("https://example.com:8080/"))

        assertEquals("example.com", gene.host.getValueAsRawString())
    }
}
