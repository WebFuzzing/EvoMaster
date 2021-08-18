package org.evomaster.e2etests.spring.rest.basic

import com.foo.spring.rest.mysql.basic.BasicController
import org.evomaster.core.problem.rest.HttpVerb
import org.evomaster.core.problem.rest.service.RestSampler
import org.evomaster.core.search.gene.IntegerGene
import org.evomaster.core.search.gene.LongGene
import org.evomaster.core.search.gene.StringGene
import org.evomaster.e2etests.utils.RestTestBase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class BasicEMTest : RestTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun initClass() {
            initClass(BasicController())
        }
    }

    @Test
    fun testRunEM() {

        runTestHandlingFlakyAndCompilation(
            "BasicEM",
            "org.bar.mysql.BasicEM",
            100
        ) { args ->
            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)

            assertHasAtLeastOne(solution, HttpVerb.GET, 400)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200)
        }
    }

    @Test
    fun testUnsignedNumeric(){

        runTestHandlingFlaky(
            "BasicUnsignedEM",
            "org.bar.mysql.BasicUnsignedEM",
            1,
            false
        ) { args ->

            val injector = init(args)

            val sampler = injector.getInstance(RestSampler::class.java)
            val dbactions = sampler.sampleSqlInsertion("X", setOf("*"))

            assertEquals(1, dbactions.size)

            val genes = dbactions[0].seeGenes()

            val a = genes.find { it.name.equals("a", ignoreCase = true) }
            assertTrue(a is IntegerGene)
            assertEquals(0, (a as IntegerGene).min)
            assertEquals(255, a.max)
            val b = genes.find { it.name.equals("b", ignoreCase = true) }
            assertTrue(b is LongGene)

            assertEquals(0L, (b as LongGene).min)
            assertEquals(4294967295L, b.max)
        }

    }
}