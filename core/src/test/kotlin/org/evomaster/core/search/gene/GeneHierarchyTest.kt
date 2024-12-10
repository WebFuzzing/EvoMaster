package org.evomaster.core.search.gene

import org.evomaster.core.search.gene.root.CompositeGene
import org.evomaster.core.search.gene.root.SimpleGene
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.reflect.full.isSuperclassOf

class GeneHierarchyTest  : AbstractGeneTest(){

    @Test
    fun testHierarchy() {

        val errors = geneClasses.filter {
            it != Gene::class && !SimpleGene::class.isSuperclassOf(it) && !CompositeGene::class.isSuperclassOf(it)
        }

        if (errors.isNotEmpty()) {
            println("Wrong names: $errors")
        }
        assertEquals(0, errors.size)
    }
}
