package org.evomaster.e2etests.spring.openapi.v3.flakinessdetect

import com.foo.rest.examples.spring.openapi.v3.flakinessdetect.FlakinessDetectController
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class FlakinessDetectWithMultiExecEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(FlakinessDetectController())
        }
    }

    @Test
    fun testRunEMWithMultipleFlakinessExecutions() {

        val outputFolder = "FlakinessDetectMultipleExecutionsEM"
        val outputClass = "org.foo.FlakinessDetectMultipleExecutionsEM"

        runTestHandlingFlakyAndCompilation(
            outputFolder,
            outputClass,
            30,
            true
        ) { args: MutableList<String> ->

            setOption(args, "minimize", "true")
            setOption(args, "handleFlakiness", "true")
            setOption(args, "execNumForDetectFlakiness", "2")
            setOption(args, "endpointFocus", "/api/flakinessdetect/multiexecution")

            val solution = initAndRun(args)

            assertTrue(solution.individuals.isNotEmpty())
            assertTextInTests(outputFolder, outputClass, "Flaky value of field \"'first'\"")
            assertTextInTests(outputFolder, outputClass, "Flaky value of field \"'second'\"")
        }
    }
}
