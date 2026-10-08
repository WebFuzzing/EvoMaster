package org.evomaster.e2etests.spring.rest.bb.chainedids


import com.foo.rest.examples.bb.chainedids.BBChainedIdsApplication
import com.foo.rest.examples.bb.chainedids.BBChainedIdsController
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.rest.bb.SpringTestBase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class BBChainedIdsEMTest : SpringTestBase() {

    companion object {
        init {
            shouldApplyInstrumentation = false
        }

        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(BBChainedIdsController())
        }
    }

    @ParameterizedTest
    @EnumSource
    fun testBlackBoxOutput(outputFormat: OutputFormat) {

        executeAndEvaluateBBTest(
            outputFormat,
            "chainedids",
            100,
            3,
            listOf("OKX","OKY")
        ){ args: MutableList<String> ->

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.POST, 201, "/api/chainedids/x", null)
            assertHasAtLeastOne(solution, HttpVerb.GET, 404, "/api/chainedids/x/{id}", null)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/chainedids/x/{id}", null)

            assertHasAtLeastOne(solution, HttpVerb.POST, 201, "/api/chainedids/y/", null)
            assertHasAtLeastOne(solution, HttpVerb.GET, 404, "/api/chainedids/y/{id}", null)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/chainedids/y/{id}", null)

            //make sure EM is not "cheating" by creating hard-coded ids
            //also, the id resolution should work fine for "clean-up" DELETE as well
            assertEquals(0, BBChainedIdsApplication.size(), "Cleanup DELETE should had been always called")
        }
    }
}