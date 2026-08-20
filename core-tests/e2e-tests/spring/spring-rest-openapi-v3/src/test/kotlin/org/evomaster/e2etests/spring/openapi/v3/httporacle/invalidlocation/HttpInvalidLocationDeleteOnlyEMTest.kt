package org.evomaster.e2etests.spring.openapi.v3.httporacle.invalidlocation

import com.foo.rest.examples.spring.openapi.v3.httporacle.invalidlocation.deleteonly.HttpInvalidLocationDeleteOnlyController
import org.evomaster.core.problem.enterprise.DetectedFaultUtils
import org.evomaster.core.problem.enterprise.ExperimentalFaultCategory
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class HttpInvalidLocationDeleteOnlyEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(HttpInvalidLocationDeleteOnlyController())
        }
    }


    @Test
    fun testRunEM() {

        runTestHandlingFlakyAndCompilation(
                "HttpInvalidLocationDeleteOnlyEM",
                20
        ) { args: MutableList<String> ->

            setOption(args, "security", "false")
            setOption(args, "schemaOracles", "false")
            setOption(args, "httpOracles", "true")
            setOption(args, "useExperimentalOracles", "true")

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)

            // The Location points to a resource declared only for DELETE (no GET), so a GET
            // would be 405. The oracle must probe with DELETE and flag the 404 it returns.
            val faults = DetectedFaultUtils.getDetectedFaultCategories(solution)
            assertTrue(ExperimentalFaultCategory.HTTP_INVALID_LOCATION in faults)

            val locationFaults = DetectedFaultUtils.getDetectedFaults(solution)
                .filter { it.category == ExperimentalFaultCategory.HTTP_INVALID_LOCATION }
            assertTrue(locationFaults.any { it.operationId.contains("/api/products/") })
        }
    }
}
