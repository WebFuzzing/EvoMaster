package org.evomaster.e2etests.spring.openapi.v3.flakinessdetect

import com.foo.rest.examples.spring.openapi.v3.flakinessdetect.FlakinessDetectController
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.function.Predicate

class FlakinessDetectBlackboxEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(FlakinessDetectController())
        }
    }


    @Test
    fun testRunEM() {
        defaultSeed = 123

        val outputFolder = "FlakinessDetectBlackboxEM"
        val outputClass = "org.foo.FlakinessDetectBlackboxEM"
        val flakyMark = "Flaky"

        runTestHandlingFlakyAndCompilation(
            outputFolder,
            outputClass,
            100
        ) { args: MutableList<String> ->



            setOption(args, "handleFlakiness", "true")

            // we may still need to specify info in non bb-e2etest
            setOption(args, "blackBox", "true")
            setOption(args, "bbTargetUrl", baseUrlOfSut)
            setOption(args, "bbSwaggerUrl", "$baseUrlOfSut/v3/api-docs")


            val solution = initAndRun(args)

            assertTrue(solution.individuals.isNotEmpty())
            assertTextInTests(outputFolder,outputClass,flakyMark)
            assertCountTextInTests(outputFolder,outputClass,
                Predicate { it: String? -> it != null && it.contains(flakyMark) }, 3)

        }
    }
}