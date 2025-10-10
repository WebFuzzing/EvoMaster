package org.evomaster.e2etests.spring.rest.bb.cleanupmismatch

import com.foo.rest.examples.bb.cleanupmismatch.BBCleanUpMismatchApplication
import com.foo.rest.examples.bb.cleanupmismatch.BBCleanUpMismatchController
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.rest.bb.SpringTestBase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class BBCleanUpMismatchEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(BBCleanUpMismatchController())
        }
    }


    @ParameterizedTest
    @EnumSource
    fun testBlackBoxOutput(outputFormat: OutputFormat) {

        executeAndEvaluateBBTest(
            outputFormat,
            "cleanupmismatch",
            200,
            3,
            setOf("POST","DELETE")
        ){ args: MutableList<String> ->

            BBCleanUpMismatchApplication.data.clear()
            assertEquals(0, BBCleanUpMismatchApplication.data.size)

            setOption(args, "blackBoxCleanUp", "true")

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.POST, 201, "/api/cleanupmismatch/items", null)

            assertEquals(0, BBCleanUpMismatchApplication.data.size)
        }

        //even after executing generated tests, should be empty
        assertEquals(0, BBCleanUpMismatchApplication.data.size)
    }


}
