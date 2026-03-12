package org.evomaster.core.search.gene.uri

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UriGeneTest {

    @Test
    fun testSetValueBasedOnForUriDataGene() {
        val gene = UriGene("data:content/type;base64,")

        assertTrue(gene.unsafeSetFromStringValue("data:text/plain;charset=UTF-8;base64,R0lGODdh"))

        // TODO: Charset value is not handled in UriDataGene.
        //  If the encoded string uses a different Charset test will fail,
        //  since the Base64StringGene.unsafeSetFromStringValue() use UTF_8 to decode the value.
        assertEquals("data:text/plain;base64,R0lGODdh", gene.getValueAsRawString())
    }
}
