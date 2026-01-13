package org.evomaster.e2etests.spring.openapi.v3.flakinessdetect

import com.foo.rest.examples.spring.openapi.v3.flakinessdetect.FlakinessDetectController
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Paths

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
        runTestHandlingFlakyAndCompilation(
            "FlakinessDetectEM",
            "org.foo.FlakinessDetectEM",
            5
        ) { args: MutableList<String> ->

            val executedMainActionToFile = "target/em-tests/FlakinessDetectEM/org/foo/FlakinessDetectEM.kt"

            args.add("--minimize")
            args.add("true")
            args.add("--detectFlakiness")
            args.add("true")


            val solution = initAndRun(args)

            val size = Files.readAllLines(Paths.get(executedMainActionToFile)).count { !it.contains("Flaky") && it.isNotBlank() }
            assertTrue(size >= 3)
        }
    }
}