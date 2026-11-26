package org.evomaster.e2etests.spring.openapi.v3.flakinessdetect

import com.foo.rest.examples.spring.openapi.v3.flakinessdetect.FlakinessDetectController
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class FlakinessDetectRestartEMTest : SpringTestBase() {
    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(FlakinessDetectController())
        }
    }

    @Test
    fun detectsAValueThatIsStableUntilTheSutRestarts() {
        val folder = "FlakinessDetectRestartEM"
        val output = "org.foo.FlakinessDetectRestartEM"
        runTestHandlingFlakyAndCompilation(folder, output, 30, true) { args ->
            setOption(args, "handleFlakiness", "true")
            setOption(args, "enableStaticFlakyInference", "false")
            setOption(args, "execNumForDetectFlakiness", "2")
            setOption(args, "endpointFocus", "/api/flakinessdetect/lifecycle")
            val solution = initAndRun(args)
            assertTrue(solution.individuals.isNotEmpty())
            assertTextInTests(folder, output, "Flaky value of field 'startupValue'")
            assertTextInTests(folder, output) {
                it.trim().startsWith("// .body(\"'startupValue'\"")
            }
            assertTextInTests(folder, output) {
                it.trim().startsWith(".body(\"'stable'\", containsString(\"same\"))")
            }
        }
    }
}
