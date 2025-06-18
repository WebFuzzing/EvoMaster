package org.evomaster.e2etests.spring.openapi.v3.timeout

import com.foo.rest.examples.spring.openapi.v3.timeout.TimeoutController
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

/**
 *
 */

class TimeoutEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            val config = EMConfig()
            config.instrumentMR_EXT_0 = false
            initClass(TimeoutController(), config)
        }
    }



    @Test
    fun testRunEM() {

        runTestHandlingFlakyAndCompilation(
                "TimeoutEM",
                "org.foo.TimeoutEM",
                1
        ) { args: MutableList<String> ->

            args.add("--tcpTimeoutMs")
            args.add("1000")
            args.add("--addPreDefinedTests")
            args.add("false")
            args.add("--instrumentMR_EXT_0")
            args.add("false") //avoid replace Thread.sleep
            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)
            assertNone(solution, HttpVerb.GET, 200)
        }
    }


}