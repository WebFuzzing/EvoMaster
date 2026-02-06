package org.evomaster.e2etests.spring.openapi.v3.flakinessdetect

import com.foo.rest.examples.spring.openapi.v3.flakinessdetect.FlakinessDetectController
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Paths

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

        runTestHandlingFlakyAndCompilation(
            "FlakinessDetectBlackboxEM",
            "org.foo.FlakinessDetectBlackboxEM",
            100
        ) { args: MutableList<String> ->

            val executedMainActionToFile = "target/em-tests/FlakinessDetectBlackboxEM/org/foo/FlakinessDetectBlackboxEM.kt"

            setOption(args, "handleFlakiness", "true")
            setOption(args, "blackBox", "true")
            setOption(args, "bbTargetUrl", baseUrlOfSut)
            setOption(args, "bbSwaggerUrl", "$baseUrlOfSut/v3/api-docs")


            val solution = initAndRun(args)

            val size = Files.readAllLines(Paths.get(executedMainActionToFile)).count { !it.contains("Flaky") && it.isNotBlank() }
            assertTrue(size >= 3)
        }
    }
}