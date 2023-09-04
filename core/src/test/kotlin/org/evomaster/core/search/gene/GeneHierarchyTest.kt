package org.evomaster.core.search.gene

import org.evomaster.core.search.gene.root.CompositeGene
import org.evomaster.core.search.gene.root.SimpleGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.reflect.full.isSuperclassOf

class GeneHierarchyTest  : AbstractGeneTest(){

    private val genes = GeneSamplerForTests.geneClasses

    @Test
    fun testHierarchy() {

        val errors = genes.filter {
            it != Gene::class && !SimpleGene::class.isSuperclassOf(it) && !CompositeGene::class.isSuperclassOf(it)
        }

        if (errors.isNotEmpty()) {
            println("Wrong names: $errors")
        }
        assertEquals(0, errors.size)
    }
}