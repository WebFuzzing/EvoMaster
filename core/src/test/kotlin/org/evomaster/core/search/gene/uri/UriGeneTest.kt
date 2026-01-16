package org.evomaster.core.search.gene.uri

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UriGeneTest {

    @Test
    fun testSetValueBasedOn() {
        val gene = UriGene("data:content/type;base64,")

//        assertEquals("http://0.0.0.0:0/", gene.getValueAsRawString())
        assertTrue(gene.unsafeSetFromStringValue("data:text/plain;charset=UTF-8;base64,R0lGODdh"))

        assertEquals("127.0.0.1", gene.getValueAsRawString())
    }
}
