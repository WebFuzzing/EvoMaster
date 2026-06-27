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
import javax.ws.rs.POST

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
            //Unfortunately, we cannot test this, at least in SpringBoot, as HTTP server will automatically
            // strip the body if status is 204. it seems this cannot be configured
            //assertHasAtLeastOne(solution, HttpVerb.GET, 204, "/api/statusoracle/no-204-if-content", "Hello")
            //assertTrue(faultsCategories.any { it == ExperimentalFaultCategory.HTTP_STATUS_NO_204_IF_CONTENT })

            //304
            assertHasAtLeastOne(solution, HttpVerb.POST, 304, "/api/statusoracle/no-304-if-no-get-or-head", null)
            assertTrue(faultsCategories.any { it == ExperimentalFaultCategory.HTTP_STATUS_NO_304_IF_NO_GET_OR_HEAD })

            //401 and 403
            assertHasAtLeastOne(solution, HttpVerb.GET, 401, "/api/statusoracle/no-401-if-no-auth", null)
            assertHasAtLeastOne(solution, HttpVerb.GET, 403, "/api/statusoracle/no-403-if-no-401", null)
            assertTrue(faultsCategories.any { it == ExperimentalFaultCategory.HTTP_STATUS_NO_401_IF_NO_AUTH })
            assertTrue(faultsCategories.any { it == ExperimentalFaultCategory.HTTP_STATUS_NO_403_IF_NO_401 })
            assertTrue(faultsCategories.any { it == ExperimentalFaultCategory.HTTP_STATUS_NO_401_IF_NO_WWW_AUTHENTICATE })

            //405
            assertHasAtLeastOne(solution, HttpVerb.GET, 405, "/api/statusoracle/no-405-if-no-allow", null)
            assertTrue(faultsCategories.any{ it == ExperimentalFaultCategory.HTTP_STATUS_NO_405_IF_NO_ALLOW})

            //406
            assertHasAtLeastOne(solution, HttpVerb.POST, 406, "/api/statusoracle/has-406-if-accept", null)
            assertTrue(faultsCategories.any { it == ExperimentalFaultCategory.HTTP_STATUS_HAS_406_IF_ACCEPT })

            //413, 415
            assertHasAtLeastOne(solution, HttpVerb.POST, 413, "/api/statusoracle/no-413-if-no-payload", null)
            assertHasAtLeastOne(solution, HttpVerb.POST, 415, "/api/statusoracle/no-415-if-no-payload", null)
            assertTrue(faultsCategories.any { it == ExperimentalFaultCategory.HTTP_STATUS_NO_413_IF_NO_PAYLOAD })
            assertTrue(faultsCategories.any { it == ExperimentalFaultCategory.HTTP_STATUS_NO_415_IF_NO_PAYLOAD })

            //501
            assertHasAtLeastOne(solution, HttpVerb.GET, 501, "/api/statusoracle/no-501-if-implemented", null)
            assertTrue(faultsCategories.any { it == ExperimentalFaultCategory.HTTP_STATUS_NO_501_IF_IMPLEMENTED })
        }
    }
}