package org.evomaster.e2etests.spring.rest.bb.inputs

import com.foo.rest.examples.bb.inputs.BBInputsController
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.rest.bb.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class BBInputsEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(BBInputsController())
        }
    }

    @ParameterizedTest
    @EnumSource
    fun testBlackBoxOutput(outputFormat: OutputFormat) {

        executeAndEvaluateBBTest(
            outputFormat,
            "inputs",
            500,
            3,
            listOf("A","B","C","D","E","negative","true","false","foo","bar","42","1234")
        ){ args: MutableList<String> ->

            setOption(args, "bbSwaggerUrl", "$baseUrlOfSut/openapi-bbinputs.json")
            setOption(args, "algorithm", "SMARTS")
            setOption(args, "advancedBlackBoxCoverage", "true")

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/bbinputs", null)
        }
    }

}
