package org.evomaster.e2etests.spring.openapi.v3.httporacle.timeout

import com.foo.rest.examples.spring.openapi.v3.httporacle.timeout.HttpTimeoutController
import org.evomaster.core.problem.enterprise.DetectedFaultUtils
import org.evomaster.core.problem.enterprise.ExperimentalFaultCategory
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class HttpTimeoutEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(HttpTimeoutController())
        }
    }


    @Test
    fun testRunEM() {

        runTestHandlingFlakyAndCompilation(
                "HttpTimeoutEM",
                5
        ) { args: MutableList<String> ->

            setOption(args, "blackBox", "true")
            setOption(args, "bbTargetUrl", baseUrlOfSut)
            setOption(args, "bbSwaggerUrl", "$baseUrlOfSut/v3/api-docs")
            setOption(args, "useExperimentalOracles", "true")
            setOption(args, "tcpTimeoutMs", "2000")

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)

            val faults = DetectedFaultUtils.getDetectedFaultCategories(solution)
            assertTrue(ExperimentalFaultCategory.HTTP_TIMEOUT in faults)

            val timeoutFaults = DetectedFaultUtils.getDetectedFaults(solution)
                .filter { it.category == ExperimentalFaultCategory.HTTP_TIMEOUT }

            // fault on the slow path
            assertTrue(timeoutFaults.any { it.operationId.contains("/api/timeout/slow/") })
            // no false positive on the fast path
            assertTrue(timeoutFaults.none { it.operationId.contains("/api/timeout/fast/") })
        }
    }
}
