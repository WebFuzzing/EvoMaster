package org.evomaster.e2etests.spring.rest.bb.defaultvalues

import com.foo.rest.examples.bb.defaultvalues.BBDefaultController
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.problem.rest.HttpVerb
import org.evomaster.e2etests.spring.rest.bb.SpringTestBase
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class BBDefaultEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(BBDefaultController())
        }
    }


    @Test
    fun testRunEMOk() {
        runTestHandlingFlakyAndCompilation(
                "BBDefaultEMOk",
                "org.foo.BBDefaultEMOk",
                20
        ) { args: MutableList<String> ->

            addBlackBoxOptions(args, OutputFormat.KOTLIN_JUNIT_5)
            setOption(args, "bbSwaggerUrl", "$baseUrlOfSut/openapi-bbdefault.json")
            setOption(args, "probRestDefault", "0.5")

            val solution = initAndRun(args)

            Assertions.assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/bbdefault", "OK")
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/bbdefault/{x}", "OK")
        }
    }

    @Test
    fun testRunEMFail() {
        runTestHandlingFlakyAndCompilation(
                "BBDefaultEMFail",
                "org.foo.BBDefaultEMFail",
                20
        ) { args: MutableList<String> ->

            addBlackBoxOptions(args, OutputFormat.KOTLIN_JUNIT_5)
            setOption(args, "bbSwaggerUrl", "$baseUrlOfSut/openapi-bbdefault.json")
            // no way in BB should be able to get the right string with no further info
            setOption(args, "probRestDefault", "0.0")

            val solution = initAndRun(args)

            Assertions.assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/bbdefault", null)
            assertNone(solution, HttpVerb.GET,200,"/api/bbdefault", "OK")
            assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/bbdefault/{x}", null)
            assertNone(solution, HttpVerb.GET,200,"/api/bbdefault/{x}", "OK")
        }
    }

}