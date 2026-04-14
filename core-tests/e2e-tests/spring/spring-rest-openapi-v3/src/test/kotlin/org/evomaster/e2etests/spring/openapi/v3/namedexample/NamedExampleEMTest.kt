package org.evomaster.e2etests.spring.openapi.v3.namedexample

import com.foo.rest.examples.spring.openapi.v3.namedexample.NamedExampleController
import junit.framework.TestCase.assertTrue
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.core.search.service.IdMapper
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class NamedExampleEMTest : SpringTestBase(){

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(NamedExampleController())
        }
    }

    @Test
    fun testRunEM() {
        runTestHandlingFlakyAndCompilation(
            "NamedExampleEM",
            200,
        ) { args: MutableList<String> ->

            val (injector, solution) = initAndDebug(args)

            Assertions.assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/namedexample", "OK")

            val fv = solution.overall

            val idMapper = injector.getInstance(IdMapper::class.java)
            val covered = fv.coveredTargets().any{ IdMapper.isNamedExample(idMapper.getDescriptiveId(it))}
            Assertions.assertTrue(covered)
        }
    }

}
