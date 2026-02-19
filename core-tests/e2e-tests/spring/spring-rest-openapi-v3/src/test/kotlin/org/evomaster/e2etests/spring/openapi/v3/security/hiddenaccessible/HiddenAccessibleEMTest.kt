package org.evomaster.e2etests.spring.openapi.v3.security.hiddenaccessible

import com.foo.rest.examples.spring.openapi.v3.security.hiddenaccessible.HiddenAccessibleController
import org.evomaster.core.problem.enterprise.DetectedFaultUtils
import org.evomaster.core.problem.enterprise.ExperimentalFaultCategory
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull

class HiddenAccessibleEMTest : SpringTestBase(){

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(HiddenAccessibleController())
        }
    }


    @Test
    fun testRunEM() {

        runTestHandlingFlakyAndCompilation(
                "HiddenAccessibleEM",
                20
        ) { args: MutableList<String> ->

            setOption(args, "security", "true")
            setOption(args, "useExperimentalOracles", "true")

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)

            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/resources", "OK")
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/resources/{id}", "OK")
            assertHasAtLeastOne(solution, HttpVerb.OPTIONS, 200, "/api/resources", null)
            assertHasAtLeastOne(solution, HttpVerb.OPTIONS, 200, "/api/resources/{id}", null)


            val faults = DetectedFaultUtils.getDetectedFaults(solution)
            assertTrue(faults.size >= 2)

            val hidden = faults.filter{it.category == ExperimentalFaultCategory.HIDDEN_ACCESSIBLE_ENDPOINT}
            assertEquals(2, hidden.size)

            assertNotNull(hidden.find { it.operationId == "GET:/api/resources" })
            assertNotNull(hidden.find { it.operationId == "DELETE:/api/resources/{id}" })
        }
    }
}
