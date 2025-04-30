package org.evomaster.e2etests.spring.rest.bb.links

import com.foo.rest.examples.bb.links.BBLinksController
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.rest.bb.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class BBLinksEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(BBLinksController())
        }
    }

    @ParameterizedTest
    @EnumSource
    fun testBlackBoxOutput(outputFormat: OutputFormat) {

        executeAndEvaluateBBTest(
            outputFormat,
            "links",
            500,
            3,
            listOf("WRONG","OK")
        ){ args: MutableList<String> ->

            setOption(args, "bbSwaggerUrl", "$baseUrlOfSut/openapi-links.json")
            setOption(args, "algorithm", "SMARTS")
            setOption(args, "probUseRestLinks", "0.5")
            setOption(args, "enableBasicAssertions", "false") //due to dynamic "code" field which would lead to flakyness
            setOption(args, "advancedBlackBoxCoverage", "true")

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/links/users/{name}/{code}", "WRONG")
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/links/users/{name}/{code}", "OK")
        }
    }
}
