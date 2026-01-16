package org.evomaster.core.search.gene.uri

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class UriGeneTest {

    @Disabled("Work in progress")
    @Test
    fun testSetValueBasedOnForUriDataGene() {
        val gene = UriGene("data:content/type;base64,")

        assertTrue(gene.unsafeSetFromStringValue("data:text/plain;charset=UTF-8;base64,R0lGODdh"))
    }
}
