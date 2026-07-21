package org.evomaster.e2etests.spring.openapi.v3.httporacle.invalidallow

import com.foo.rest.examples.spring.openapi.v3.httporacle.invalidallow.missing.HttpMissingAllowController
import org.evomaster.core.problem.enterprise.DetectedFaultUtils
import org.evomaster.core.problem.enterprise.ExperimentalFaultCategory
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class HttpMissingAllowEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(HttpMissingAllowController())
        }
    }


    @Test
    fun testRunEM() {

        runTestHandlingFlakyAndCompilation(
                "HttpMissingAllowEM",
                20
        ) { args: MutableList<String> ->

            setOption(args, "security", "false")
            setOption(args, "schemaOracles", "false")
            setOption(args, "httpOracles", "true")
            setOption(args, "useExperimentalOracles", "true")

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)

            val faults = DetectedFaultUtils.getDetectedFaultCategories(solution)
            assertTrue(ExperimentalFaultCategory.HTTP_INVALID_ALLOW in faults)

            val allowFaults = DetectedFaultUtils.getDetectedFaults(solution)
                .filter { it.category == ExperimentalFaultCategory.HTTP_INVALID_ALLOW }

            assertTrue(allowFaults.any { it.operationId.contains("/api/products/") })
            assertTrue(allowFaults.none { it.operationId.contains("/api/orders/") })
        }
    }
}