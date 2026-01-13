package org.evomaster.e2etests.spring.rest.bb.authextractheader

import com.foo.rest.examples.bb.authextractheader.AuthExtractHeaderController
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.rest.bb.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class BBAuthExtractHeaderEMTest : SpringTestBase() {

    companion object {
        init {
            shouldApplyInstrumentation = false
        }

        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(AuthExtractHeaderController())
        }
    }

    @ParameterizedTest
    @EnumSource
    fun testBlackBoxOutput(outputFormat: OutputFormat) {

        executeAndEvaluateBBTest(
            outputFormat,
            "authextractheader",
            20,
            3,
            "OK"
        ){ args: MutableList<String> ->

            setOption(args, "configPath","src/test/resources/config/authextractheader.yaml")
            setOption(args, "endpointFocus", "/api/logintokenextractheader/check")

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)
            assertNone(solution, HttpVerb.POST, 200)
            assertNone(solution, HttpVerb.POST, 400)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/logintokenextractheader/check", "OK")
        }
    }
}