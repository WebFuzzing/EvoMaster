package org.evomaster.e2etests.spring.openapi.v3.flakinessdetect

import com.foo.rest.examples.spring.openapi.v3.flakinessdetect.FlakinessDetectController
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Paths
import java.util.function.Predicate

class FlakinessDetectEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(FlakinessDetectController())
        }
    }


    @Test
    fun testRunEM() {

        val outputFolder = "FlakinessDetectEM"
        val outputClass = "org.foo.FlakinessDetectEM"
        val flakyMark = "Flaky"

        runTestHandlingFlakyAndCompilation(
            outputFolder,
            outputClass,
            100
        ) { args: MutableList<String> ->

            setOption(args, "minimize", "true")
            setOption(args, "handleFlakiness", "true")


            val solution = initAndRun(args)

            assertTrue(solution.individuals.isNotEmpty())
            assertTextInTests(outputFolder,outputClass,flakyMark)
            assertCountTextInTests(outputFolder,outputClass,
                Predicate { it: String? -> it != null && it.contains(flakyMark) }, 3)
        }
    }
}