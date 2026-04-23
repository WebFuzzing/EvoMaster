package org.evomaster.e2etests.spring.openapi.v3.security.notrecognized

import com.foo.rest.examples.spring.openapi.v3.security.notrecognized.NotRecognizedController
import com.webfuzzing.commons.faults.DefinedFaultCategory
import com.webfuzzing.commons.faults.FaultCategory
import org.evomaster.core.problem.enterprise.DetectedFaultUtils
import org.evomaster.core.problem.enterprise.ExperimentalFaultCategory
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class SecurityNotRecognizedEMTest : SpringTestBase(){

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(NotRecognizedController())
        }
    }


    @Test
    fun testRunEM() {

        runTestHandlingFlakyAndCompilation(
            "SecurityNotRecognizedEM",
            200
        ) { args: MutableList<String> ->

            setOption(args, "security", "true")
            setOption(args, "schemaOracles", "false")

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)

            assertHasAtLeastOne(solution, HttpVerb.PUT, 201, "/api/resources/{id}", null)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/resources/{id}", null)
            assertHasAtLeastOne(solution, HttpVerb.GET, 403, "/api/resources/{id}", null)
            assertHasAtLeastOne(solution, HttpVerb.POST, 401, "/api/resources/", null)

            val faults = DetectedFaultUtils.getDetectedFaultCategories(solution)

            assertEquals(1, faults.size)
            assertEquals(DefinedFaultCategory.SECURITY_NOT_RECOGNIZED_AUTHENTICATED, faults.first())
        }
    }
}
