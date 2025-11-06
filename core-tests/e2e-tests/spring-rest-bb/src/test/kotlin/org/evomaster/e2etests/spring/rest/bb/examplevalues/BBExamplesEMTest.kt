package org.evomaster.e2etests.spring.rest.bb.examplevalues

import com.foo.rest.examples.bb.examplevalues.BBExamplesController
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.rest.bb.SpringTestBase
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class BBExamplesEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(BBExamplesController())
        }
    }

    @ParameterizedTest
    @EnumSource
    fun testBlackBoxOutput(outputFormat: OutputFormat) {

        executeAndEvaluateBBTest(
            outputFormat,
            "examplevalues",
            500,
            3,
            listOf("A","B","C","D")
        ){ args: MutableList<String> ->

            setOption(args, "bbSwaggerUrl", "$baseUrlOfSut/openapi-bbexamples.json")
            setOption(args, "probRestDefault", "0.45")
            setOption(args, "probRestExamples","0.45")
            setOption(args, "addTestComments", "true")

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)
        }
    }

    @Test
    fun testRunEMOk() {
        runTestHandlingFlakyAndCompilation(
                "BBExamplesEMOk",
                "org.foo.BBExamplesEMOk",
                500
        ) { args: MutableList<String> ->

            addBlackBoxOptions(args, OutputFormat.KOTLIN_JUNIT_5)
            setOption(args, "bbSwaggerUrl", "$baseUrlOfSut/openapi-bbexamples.json")
            setOption(args, "probRestDefault", "0.45")
            setOption(args, "probRestExamples","0.45")


            val solution = initAndRun(args)

            Assertions.assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/bbexamples", "OK")
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/bbexamples/{x}", "OK")
            //NOTE: as "default" is only applicable for optional params, and path params are always required, parser might skip it...
//            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/bbexamples/{x}/mixed", "12345")
            assertHasAtLeastOne(solution, HttpVerb.GET, 201, "/api/bbexamples/{x}/mixed", "456789")
            assertHasAtLeastOne(solution, HttpVerb.GET, 202, "/api/bbexamples/{x}/mixed", "778899")
            assertHasAtLeastOne(solution, HttpVerb.GET, 203, "/api/bbexamples/{x}/mixed", "Foo")
            assertHasAtLeastOne(solution, HttpVerb.GET, 250, "/api/bbexamples/{x}/mixed", "Bar")
            assertHasAtLeastOne(solution, HttpVerb.GET, 251, "/api/bbexamples/{x}/mixed", "Hello")
        }
    }

    @Test
    fun testRunEMFail() {
        runTestHandlingFlakyAndCompilation(
                "BBExamplesEMFail",
                "org.foo.BBExamplesEMFail",
                50
        ) { args: MutableList<String> ->

            addBlackBoxOptions(args, OutputFormat.KOTLIN_JUNIT_5)
            setOption(args, "bbSwaggerUrl", "$baseUrlOfSut/openapi-bbexamples.json")
            setOption(args, "probRestExamples", "0.0")

            val solution = initAndRun(args)

            Assertions.assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/bbexamples", null)
            assertNone(solution, HttpVerb.GET,200,"/api/bbexamples", "OK")
            assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/bbexamples/{x}", null)
            assertNone(solution, HttpVerb.GET,200,"/api/bbexamples/{x}", "OK")
        }
    }

}
