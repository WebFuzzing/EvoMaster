package org.evomaster.e2etests.spring.rest.bb.httptimeout

import com.foo.rest.examples.bb.httptimeout.BBHttpTimeoutController
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.problem.enterprise.DetectedFaultUtils
import org.evomaster.core.problem.enterprise.ExperimentalFaultCategory
import org.evomaster.e2etests.spring.rest.bb.SpringTestBase
import org.evomaster.e2etests.utils.EnterpriseTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class BBHttpTimeoutEMTest : SpringTestBase() {

    companion object {

        init {
            EnterpriseTestBase.shouldApplyInstrumentation = false
        }

        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(BBHttpTimeoutController())
        }
    }

    @ParameterizedTest
    @EnumSource
    fun testBlackBoxOutput(outputFormat: OutputFormat) {

        executeAndEvaluateBBTest(
            outputFormat,
            "bbhttptimeout",
            5,
            6,
            "timeout"
        ) { args: MutableList<String> ->

            setOption(args, "useExperimentalOracles", "true")
            setOption(args, "tcpTimeoutMs", "2000")
            setOption(args, "httpOracles", "true")

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)

            val timeoutFaults = DetectedFaultUtils.getDetectedFaults(solution)
                .filter { it.category == ExperimentalFaultCategory.HTTP_TIMEOUT }

            // fault on the slow path
            assertTrue(timeoutFaults.any { it.operationId.contains("/api/timeout/slow/") })
            // no false positive on the fast path
            assertTrue(timeoutFaults.none { it.operationId.contains("/api/timeout/fast/") })
        }
    }
}
