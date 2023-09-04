package org.evomaster.core.search.gene


import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Assertions.assertEquals

import org.junit.jupiter.api.Test


class GeneCanSampleTest : AbstractGeneTest(){

    @Test
    fun testCanSample() {

        val errors = geneClasses
                .filter { !it.isAbstract }
                .filter {
                    try {
                        GeneSamplerForTests.sample(it, Randomness().apply { updateSeed(42) }); false
                    } catch (e: Exception) {
                        true
                    }
                }

        if (errors.isNotEmpty()) {
            println("Cannot sample: $errors")
        }
        assertEquals(0, errors.size)
    }
}
