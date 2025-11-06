package org.evomaster.e2etests.spring.rest.postgres.likecondition

import com.foo.spring.rest.postgres.likecondition.LikeConditionController
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.core.search.gene.regex.RegexGene
import org.evomaster.e2etests.utils.RestTestBase
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class LikeConditionEMTest : RestTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun initClass() {
            initClass(LikeConditionController())
        }
    }

    @Test
    fun testRunEM() {

        runTestHandlingFlakyAndCompilation(
            "LikeConditionEM",
            "org.bar.postgresql.LikeConditionEM",
            100
        ) { args ->

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)

            assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/like",null)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/like", null)


            solution.individuals
                .flatMap { it.individual.seeFullTreeGenes() }
                .filterIsInstance<RegexGene>()
                .apply {
                    assertTrue(isNotEmpty())
                    forEach {
                        assertEquals("${RegexGene.DATABASE_REGEX_PREFIX}foo%", it.sourceRegex)
                    }
                }

        }
    }
}