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
            val faultsCategories = DetectedFaultUtils.getDetectedFaultCategories(solution)
            assertTrue(faultsCategories.isNotEmpty())

            //invalid code
            assertHasAtLeastOne(solution, HttpVerb.GET, 42, "/api/statusoracle/no-non-standard-codes/42", null)
            assertHasAtLeastOne(solution, HttpVerb.GET, 912, "/api/statusoracle/no-non-standard-codes/912", null)
            assertHasAtLeastOne(solution, HttpVerb.GET, 1024, "/api/statusoracle/no-non-standard-codes/1024", null)
            assertTrue(faultsCategories.any { it == ExperimentalFaultCategory.HTTP_STATUS_NO_NON_STANDARD_CODES })

            //201
            assertHasAtLeastOne(solution, HttpVerb.GET, 201, "/api/statusoracle/no-201-if-get", null)
            assertHasAtLeastOne(solution, HttpVerb.DELETE, 201, "/api/statusoracle/no-201-if-delete", null)
            assertHasAtLeastOne(solution, HttpVerb.PATCH, 201, "/api/statusoracle/no-201-if-patch", null)
            assertTrue(faultsCategories.any { it == ExperimentalFaultCategory.HTTP_STATUS_NO_201_IF_DELETE })
            assertTrue(faultsCategories.any { it == ExperimentalFaultCategory.HTTP_STATUS_NO_201_IF_PATCH })

            //204
            assertHasAtLeastOne(solution, HttpVerb.GET, 201, "/api/statusoracle/no-204-if-content", "Hello")
            assertTrue(faultsCategories.any { it == ExperimentalFaultCategory.HTTP_STATUS_NO_204_IF_CONTENT })

            //406
            //has-406-if-accept
            assertHasAtLeastOne(solution, HttpVerb.POST, 406, "/api/statusoracle/has-406-if-accept", null)
            assertTrue(faultsCategories.any { it == ExperimentalFaultCategory.HTTP_STATUS_HAS_406_IF_ACCEPT })

            //413, 415
            assertHasAtLeastOne(solution, HttpVerb.POST, 413, "/api/statusoracle/no-413-if-no-payload", null)
            assertHasAtLeastOne(solution, HttpVerb.POST, 415, "/api/statusoracle/no-415-if-no-payload", null)
            assertTrue(faultsCategories.any { it == ExperimentalFaultCategory.HTTP_STATUS_NO_413_IF_NO_PAYLOAD })
            assertTrue(faultsCategories.any { it == ExperimentalFaultCategory.HTTP_STATUS_NO_415_IF_NO_PAYLOAD })


            //TODO
        }
    }
}