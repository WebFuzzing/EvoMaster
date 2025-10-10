package org.evomaster.e2etests.spring.rest.bb.cleanup

import com.foo.rest.examples.bb.cleanup.BBCleanUpApplication
import com.foo.rest.examples.bb.cleanup.BBCleanUpController
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.rest.bb.SpringTestBase
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class BBCleanUpEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(BBCleanUpController())
        }
    }


    @ParameterizedTest
    @EnumSource
    fun testBlackBoxOutput(outputFormat: OutputFormat) {

        executeAndEvaluateBBTest(
            outputFormat,
            "cleanup",
            200,
            3,
            setOf("POST","DELETE")
        ){ args: MutableList<String> ->

            BBCleanUpApplication.data.clear()
            assertEquals(0, BBCleanUpApplication.data.size)

            setOption(args, "blackBoxCleanUp", "true")

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.POST, 201, "/api/cleanup/items", null)

            assertEquals(0, BBCleanUpApplication.data.size)
        }

        //even after executing generated tests, should be empty
        assertEquals(0, BBCleanUpApplication.data.size)
    }


}
