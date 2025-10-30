package org.evomaster.e2etests.spring.rest.bb.chainedlocation


import com.foo.rest.examples.bb.chainedlocation.BBChainedLocationController
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.rest.bb.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class BBChainedLocationEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(BBChainedLocationController())
        }
    }

    @ParameterizedTest
    @EnumSource
    fun testBlackBoxOutput(outputFormat: OutputFormat) {

        executeAndEvaluateBBTest(
            outputFormat,
            "chainedlocation",
            50,
            3,
            "OK"
        ){ args: MutableList<String> ->

            setOption(args, "algorithm", "SMARTS") //TODO remove once default

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/chainedlocation/x/{idx}/y/{idy}/z/{idz}/value", null)

        }
    }
}