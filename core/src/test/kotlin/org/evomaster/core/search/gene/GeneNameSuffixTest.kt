package org.evomaster.core.search.gene

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GeneNameSuffixTest : AbstractGeneTest() {

    @Test
    fun testNameSuffix() {

        val errors = geneClasses.map { it.qualifiedName!! }
                .filter { !it.endsWith("Gene") }

        if (errors.isNotEmpty()) {
            println("Wrong names: $errors")
        }
        assertEquals(0, errors.size)
    }
}
