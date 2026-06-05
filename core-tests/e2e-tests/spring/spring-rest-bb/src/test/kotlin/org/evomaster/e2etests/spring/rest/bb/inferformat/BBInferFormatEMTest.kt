package org.evomaster.e2etests.spring.rest.bb.inferformat


import com.foo.rest.examples.bb.inferformat.BBInferFormatController
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.rest.bb.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class BBInferFormatEMTest : SpringTestBase() {

    companion object {
        init {
            shouldApplyInstrumentation = false
        }

        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(BBInferFormatController())
        }
    }

    @ParameterizedTest
    @EnumSource
    fun testBlackBoxOutput(outputFormat: OutputFormat) {

        executeAndEvaluateBBTest(
            outputFormat,
            "inferformat",
            200,
            3,
            listOf("uuid","uri","email","description-uuid","description-date")
        ){ args: MutableList<String> ->

            setOption(args, "enableAdvancedFormats", "false")
            setOption(args, "inferFormatFromNames", "true")

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/inferformat/uuid", "OK")
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/inferformat/uri", "OK")
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/inferformat/email", "OK")
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/inferformat/uuid", "OK")
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/inferformat/date", "OK")
        }
    }
}
