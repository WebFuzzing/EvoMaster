package org.evomaster.e2etests.spring.openapi.v3.flakinessdetect

import com.foo.rest.examples.spring.openapi.v3.flakinessdetect.FlakinessDetectController
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class FlakinessDetectSkipFieldEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(FlakinessDetectController())
        }
    }

    @Test
    fun testRunEMWithSkippedFlakyField() {

        val outputFolder = "FlakinessDetectSkippedFieldEM"
        val outputClass = "org.foo.FlakinessDetectSkippedFieldEM"

        runTestHandlingFlakyAndCompilation(
            outputFolder,
            outputClass,
            30,
            true
        ) { args: MutableList<String> ->

            setOption(args, "minimize", "true")
            setOption(args, "handleFlakiness", "false")
            setOption(args, "endpointFocus", "/api/flakinessdetect/multiexecution")
            setOption(args, "fieldsToSkipInAssertions", "first,second")

            val solution = initAndRun(args)

            assertTrue(solution.individuals.isNotEmpty())
            assertTextInTests(outputFolder, outputClass, ".body(\"'stable'\"")
            assertTextNotInTests(outputFolder, outputClass, ".body(\"'first'\"")
            assertTextNotInTests(outputFolder, outputClass, ".body(\"'second'\"")
        }
    }
}
