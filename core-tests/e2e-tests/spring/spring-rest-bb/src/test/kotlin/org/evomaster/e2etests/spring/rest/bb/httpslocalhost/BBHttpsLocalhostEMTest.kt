package org.evomaster.e2etests.spring.rest.bb.httpslocalhost

import com.foo.rest.examples.bb.httpslocalhost.BBHttpsLocalhostController
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.rest.bb.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class BBHttpsLocalhostEMTest : SpringTestBase() {

    companion object {
        init {
            shouldApplyInstrumentation = false
        }

        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(BBHttpsLocalhostController())
        }
    }

    @ParameterizedTest
    @EnumSource
    fun testBlackBoxOutput(outputFormat: OutputFormat) {
        executeAndEvaluateBBTest(
            outputFormat,
            "httpslocalhost",
            200,
            3,
            "OK"
        ){ args: MutableList<String> ->
            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/sayHello", "OK")
        }
    }


}
