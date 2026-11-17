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
            for (field in listOf("first", "second")) {
                assertTextInTests(outputFolder, outputClass, "Flaky value of field '$field'")
                assertTextInTests(outputFolder, outputClass) { line ->
                    line.trim().startsWith("// .body(\"'$field'\"")
                }
            }
            // Different test cases can observe different values in the finite replay budget.
            // Do not require every assertion on these fields in the suite to be disabled.
            // Stable assertions must remain active.
            assertTextInTests(outputFolder, outputClass) { line ->
                line.trim().startsWith(".body(\"'stable'\", containsString(\"same\"))")
            }
        }
    }
}
