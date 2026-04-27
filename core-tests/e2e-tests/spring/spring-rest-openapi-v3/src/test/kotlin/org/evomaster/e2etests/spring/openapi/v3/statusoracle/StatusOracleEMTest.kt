package org.evomaster.e2etests.spring.openapi.v3.statusoracle

import com.foo.rest.examples.spring.openapi.v3.statusoracle.StatusOracleController
import org.evomaster.core.problem.enterprise.DetectedFaultUtils
import org.evomaster.core.problem.enterprise.ExperimentalFaultCategory
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class StatusOracleEMTest : SpringTestBase(){

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(StatusOracleController())
        }
    }


    @Test
    fun testRunEM() {

        runTestHandlingFlakyAndCompilation(
                "StatusOracleEM",
                200
        ) { args: MutableList<String> ->

            setOption(args, "useExperimentalOracles", "true")
            setOption(args, "statusOracles", "true")

            val solution = initAndRun(args)

            assertTrue(solution.individuals.isNotEmpty())

            assertHasAtLeastOne(solution, HttpVerb.GET, 42, "/api/statusoracle/no-non-standard-codes/42", null)
            assertHasAtLeastOne(solution, HttpVerb.GET, 912, "/api/statusoracle/no-non-standard-codes/912", null)
            assertHasAtLeastOne(solution, HttpVerb.GET, 1024, "/api/statusoracle/no-non-standard-codes/1024", null)

            val faultsCategories = DetectedFaultUtils.getDetectedFaultCategories(solution)
            assertTrue(faultsCategories.isNotEmpty())
            assertTrue(faultsCategories.any { it == ExperimentalFaultCategory.HTTP_STATUS_NO_NON_STANDARD_CODES })

            //TODO
        }
    }
}