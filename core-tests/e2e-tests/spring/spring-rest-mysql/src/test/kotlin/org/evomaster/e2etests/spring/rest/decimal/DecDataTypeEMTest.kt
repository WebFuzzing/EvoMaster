package org.evomaster.e2etests.spring.rest.decimal

import com.foo.spring.rest.mysql.decimal.DecDataTypeController
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.core.problem.rest.data.RestIndividual
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.e2etests.utils.RestTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.evomaster.core.search.action.ActionFilter

class DecDataTypeEMTest : RestTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun initClass() {
            initClass(DecDataTypeController())
        }
    }

    @Test
    fun testRunEM() {

        runTestHandlingFlakyAndCompilation(
            "DecDataTypeEM",
            "org.bar.mysql.DecDataTypeEM",
            500
        ) { args ->

                val solution = initAndRun(args)

                assertTrue(solution.individuals.size >= 1)

                assertTrue(areAllValidGene(solution.individuals))

                assertHasAtLeastOne(solution, HttpVerb.GET, 400)
                assertHasAtLeastOne(solution, HttpVerb.GET, 200)
            }

    }

    private fun areAllValidGene(inds : MutableList<EvaluatedIndividual<RestIndividual>>): Boolean{
        return inds.all { e-> e.individual.seeTopGenes(ActionFilter.ALL).all { g-> g.flatView().all { ig-> ig.isLocallyValid() } } }
    }
}