package org.evomaster.e2etests.spring.rest.bb.jsonmergepatch

import com.foo.rest.examples.bb.jsonmergepatch.BBJsonMergePatchController
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.rest.bb.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class BBJsonMergePatchEMTest : SpringTestBase() {

    companion object {
        init {
            shouldApplyInstrumentation = false
        }

        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(BBJsonMergePatchController())
        }
    }



    @ParameterizedTest
    @EnumSource
    fun testBlackBoxOutput(outputFormat: OutputFormat) {

        executeAndEvaluateBBTest(
            outputFormat,
            "jsonmergepatch",
            100,
            3,
            listOf("BOTH_UNDEFINED", "ONE_UNDEFINED", "BOTH_NULL","BOTH_PRESENT","ONE_PRESENT")
        ){ args: MutableList<String> ->


            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.PATCH, 400, "/api/jsonmergepatch/", null)
            assertHasAtLeastOne(solution, HttpVerb.PATCH, 401, "/api/jsonmergepatch/", null)
            assertHasAtLeastOne(solution, HttpVerb.PATCH, 403, "/api/jsonmergepatch/", null)
            assertHasAtLeastOne(solution, HttpVerb.PATCH, 404, "/api/jsonmergepatch/", null)
            assertHasAtLeastOne(solution, HttpVerb.PATCH, 200, "/api/jsonmergepatch/", null)
        }
    }
}
