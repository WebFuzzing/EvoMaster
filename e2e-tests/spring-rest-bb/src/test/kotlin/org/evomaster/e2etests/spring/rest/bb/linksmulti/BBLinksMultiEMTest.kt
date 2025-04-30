package org.evomaster.e2etests.spring.rest.bb.linksmulti


import com.foo.rest.examples.bb.linksmulti.BBLinksMultiController
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.rest.bb.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class BBLinksMultiEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(BBLinksMultiController())
        }
    }

    @ParameterizedTest
    @EnumSource
    fun testBlackBoxOutput(outputFormat: OutputFormat) {

        executeAndEvaluateBBTest(
            outputFormat,
            "linksmulti",
            500,
            3,
            listOf("FOOBAR","CODE","HELLO")
        ){ args: MutableList<String> ->

            setOption(args, "bbSwaggerUrl", "$baseUrlOfSut/openapi-linksmulti.json")
            setOption(args, "algorithm", "SMARTS")
            setOption(args, "probUseRestLinks", "0.9")
            setOption(args, "advancedBlackBoxCoverage", "true")
            setOption(args, "addTestComments", "true")

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.POST, 400, "/api/linksmulti/y", null)
            assertHasAtLeastOne(solution, HttpVerb.POST, 400, "/api/linksmulti/z", null)
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/linksmulti/x", null)
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/linksmulti/y", null)
            assertHasAtLeastOne(solution, HttpVerb.POST, 201, "/api/linksmulti/x", null)
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/linksmulti/z", null)
        }
    }
}
