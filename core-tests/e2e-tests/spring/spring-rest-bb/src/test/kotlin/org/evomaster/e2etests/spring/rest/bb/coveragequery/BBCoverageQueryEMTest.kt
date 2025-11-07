package org.evomaster.e2etests.spring.rest.bb.coveragequery

import com.foo.rest.examples.bb.coveragequery.BBCoverageQueryController
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.rest.bb.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class BBCoverageQueryEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(BBCoverageQueryController())
        }
    }

    @ParameterizedTest
    @EnumSource
    fun testBlackBoxOutput(outputFormat: OutputFormat) {

        executeAndEvaluateBBTest(
            outputFormat,
            "coveragequery",
            100,
            3,
            listOf("A","B","C","D","E","!A","!B","!C","!D","!E")
        ){ args: MutableList<String> ->

            setOption(args, "algorithm", "SMARTS")
            setOption(args, "advancedBlackBoxCoverage", "true")

            val solution = initAndRun(args)

            // Need at least 3 tests, when using minimization, as each is independent
            // 1 for schema, at least 2 for different true/false combinations
            assertTrue(solution.individuals.size >= 3)

            assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/coveragequery", null)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/coveragequery", null)
        }
    }
}
