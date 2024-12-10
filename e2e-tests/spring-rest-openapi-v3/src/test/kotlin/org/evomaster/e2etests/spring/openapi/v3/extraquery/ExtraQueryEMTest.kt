package org.evomaster.e2etests.spring.openapi.v3.extraquery

import com.foo.rest.examples.spring.openapi.v3.assertions.AssertionController
import com.foo.rest.examples.spring.openapi.v3.extraquery.ExtraQueryController
import com.foo.rest.examples.spring.openapi.v3.uuid.UuidController
import org.evomaster.client.java.instrumentation.shared.ClassName
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.problem.rest.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
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

class ExtraQueryEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(ExtraQueryController())
        }
    }


    @Test
    fun testRunEM() {
        runTestHandlingFlakyAndCompilation(
            "ExtraQueryEM",
            "org.foo.ExtraQueryEM",
            1000
        ) { args: MutableList<String> ->

            args.add("--extraQueryParam")
            args.add("true")
            args.add("--searchPercentageExtraHandling")
            args.add("0.7")

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)
            assertNone(solution,HttpVerb.POST, 405) //this happens if _method is not handled
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/extraquery/servlet", "OK")
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/extraquery/proxyprint", "OK")
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/extraquery/languagetool", "OK")
        }
    }

}