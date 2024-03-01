package org.evomaster.e2etests.spring.openapi.v3.timeout

import com.foo.rest.examples.spring.openapi.v3.assertions.AssertionController
import com.foo.rest.examples.spring.openapi.v3.timeout.TimeoutController
import com.foo.rest.examples.spring.openapi.v3.wiremock.jsonarray.WmJsonArrayController
import org.evomaster.client.java.instrumentation.shared.ClassName
import org.evomaster.core.EMConfig
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.problem.rest.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertTimeoutPreemptively
import java.lang.IllegalArgumentException
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Duration
import java.util.stream.Stream

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