package org.evomaster.e2etests.spring.openapi.v3.security.pathconflict

import com.foo.rest.examples.spring.openapi.v3.security.pathconflict.PathConflictDeleteController
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class PathConflictDeleteEMTest : SpringTestBase(){

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(PathConflictDeleteController())
        }
    }


    @Test
    fun testRunEM() {

        runTestHandlingFlakyAndCompilation(
                "PathConflictDeleteEM",
                200
        ) { args: MutableList<String> ->

            setOption(args, "security", "true")
            setOption(args, "schemaOracles", "false")

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)

//            assertHasAtLeastOne(solution, HttpVerb.PUT, 201, "/api/resources/{id}", null)
//            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/resources/{id}", null)
//            assertHasAtLeastOne(solution, HttpVerb.GET, 403, "/api/resources/{id}", null)
//            assertHasAtLeastOne(solution, HttpVerb.POST, 401, "/api/resources/", null)
//
//
//            val faults = DetectedFaultUtils.getDetectedFaultCategories(solution)
//            assertEquals(1, faults.size)
//            assertEquals(FaultCategory.SECURITY_NOT_RECOGNIZED_AUTHENTICATED, faults.first())
        }
    }
}