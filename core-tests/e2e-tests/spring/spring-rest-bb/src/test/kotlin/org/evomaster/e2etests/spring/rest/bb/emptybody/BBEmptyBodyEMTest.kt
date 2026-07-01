package org.evomaster.e2etests.spring.rest.bb.emptybody

import com.foo.rest.examples.bb.emptybody.BBEmptyBodyController
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.rest.bb.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class BBEmptyBodyEMTest : SpringTestBase() {

    companion object {
        init {
            shouldApplyInstrumentation = false
        }

        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(BBEmptyBodyController())
        }
    }



    @ParameterizedTest
    @EnumSource
    fun testBlackBoxOutput(outputFormat: OutputFormat) {

        executeAndEvaluateBBTest(
            outputFormat,
            "emptybody",
            100,
            3,
            listOf("PATCH","PUT","POST")
        ){ args: MutableList<String> ->

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.PATCH, 200, "/api/bbemptybody/patch", "OK")
            assertNone(solution, HttpVerb.PATCH, 415, "/api/bbemptybody/patch", null)
            assertNone(solution, HttpVerb.PATCH, 500, "/api/bbemptybody/patch", null)


            assertHasAtLeastOne(solution, HttpVerb.PUT, 200, "/api/bbemptybody/put", "OK")
            assertNone(solution, HttpVerb.PUT, 415, "/api/bbemptybody/put", null)
            assertNone(solution, HttpVerb.PUT, 500, "/api/bbemptybody/put", null)

            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/bbemptybody/post", "OK")
            assertNone(solution, HttpVerb.POST, 415, "/api/bbemptybody/post", null)
            assertNone(solution, HttpVerb.POST, 500, "/api/bbemptybody/post", null)
        }
    }
}
