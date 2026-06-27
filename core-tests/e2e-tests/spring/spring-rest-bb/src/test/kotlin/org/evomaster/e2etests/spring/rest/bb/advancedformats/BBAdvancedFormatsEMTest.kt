package org.evomaster.e2etests.spring.rest.bb.advancedformats

import com.foo.rest.examples.bb.advancedformats.BBAdvancedFormatsController
import com.foo.rest.examples.bb.exampleobject.BBExampleObjectController
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.core.problem.rest.param.BodyParam
import org.evomaster.e2etests.spring.rest.bb.SpringTestBase
import org.evomaster.e2etests.utils.EnterpriseTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class BBAdvancedFormatsEMTest : SpringTestBase() {

    companion object {
        init {
            shouldApplyInstrumentation = false
        }

        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(BBAdvancedFormatsController())
        }
    }

    @ParameterizedTest
    @EnumSource
    fun testBlackBoxOutput(outputFormat: OutputFormat) {

        executeAndEvaluateBBTest(
            outputFormat,
            "advancedformats",
            100,
            3,
            listOf("uuid","uri","email")
        ){ args: MutableList<String> ->

            setOption(args, "schema", "$baseUrlOfSut/openapi-bbadvancedformats.json")
            setOption(args, "enableAdvancedFormats", "true")
            setOption(args, "inferFormatFromNames", "false")

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/advancedformats/uuid", "OK")
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/advancedformats/uri", "OK")
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/advancedformats/email", "OK")
        }
    }
}
