package org.evomaster.core.search.gene


import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test


class GenePackageTest : AbstractGeneTest() {

    @Test
    fun testPackage() {

        val errors = geneClasses.map { it.qualifiedName!! }
                .filter { !it.startsWith("org.evomaster.core.search.gene") }

        if (errors.isNotEmpty()) {
            println("Wrong packages: $errors")
        }
        assertEquals(0, errors.size)
    }
}
