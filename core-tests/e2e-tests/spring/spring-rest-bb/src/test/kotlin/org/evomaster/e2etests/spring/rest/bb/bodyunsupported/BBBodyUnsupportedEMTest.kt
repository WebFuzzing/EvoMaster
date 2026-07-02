package org.evomaster.e2etests.spring.rest.bb.bodyunsupported

import com.foo.rest.examples.bb.bodyunsupported.BBBodyUnsupportedController
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.rest.bb.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class BBBodyUnsupportedEMTest : SpringTestBase() {

    companion object {
        init {
            shouldApplyInstrumentation = false
        }

        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(BBBodyUnsupportedController())
        }
    }

    @ParameterizedTest
    @EnumSource
    fun testBlackBoxOutput(outputFormat: OutputFormat) {

        executeAndEvaluateBBTest(
            outputFormat,
            "bodyunsupported",
            100,
            3,
            listOf("OCTETS","PDF")
        ){ args: MutableList<String> ->

            setOption(args, "schema", "$baseUrlOfSut/openapi-bodyunsupported.json")

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)
            assertNone(solution, HttpVerb.POST, 415, "/api/bodyunsupported/octets", null)
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/bodyunsupported/octets", "OK")
            assertNone(solution, HttpVerb.POST, 415, "/api/bodyunsupported/pdf", null)
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/bodyunsupported/pdf", "OK")
        }
    }
}
