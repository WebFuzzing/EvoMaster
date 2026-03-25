package org.evomaster.e2etests.spring.rest.bb.sqli

import com.foo.rest.examples.bb.sqli.BBSQLiController
import com.webfuzzing.commons.faults.DefinedFaultCategory
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.problem.enterprise.DetectedFaultUtils
import org.evomaster.e2etests.spring.rest.bb.SpringTestBase
import org.evomaster.e2etests.utils.EnterpriseTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class BBSQLiEMTest : SpringTestBase() {

    companion object {

        init {
            EnterpriseTestBase.shouldApplyInstrumentation = false
        }

        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(BBSQLiController())
        }
    }

    @ParameterizedTest
    @EnumSource
    fun testBlackBoxOutput(outputFormat: OutputFormat) {

        executeAndEvaluateBBTest(
            outputFormat,
            "bbsqli",
            100,
            6,
            "sqli"
        ){ args: MutableList<String> ->

            setOption(args, "security", "true")
            setOption(args, "sqli", "true")
            setOption(args, "sqliBaselineMaxResponseTimeMs", "5000")
            setOption(args, "sqliInjectedSleepDurationMs", "8000")

            val solution = initAndRun(args)
            val faultCategories = DetectedFaultUtils.getDetectedFaultCategories(solution)

            assertTrue(solution.individuals.size >= 1)
            assertTrue({ DefinedFaultCategory.SQL_INJECTION in faultCategories })
        }
    }
}
