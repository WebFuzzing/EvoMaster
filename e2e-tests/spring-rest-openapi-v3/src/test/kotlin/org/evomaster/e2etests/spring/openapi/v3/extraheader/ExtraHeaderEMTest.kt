package org.evomaster.e2etests.spring.openapi.v3.extraheader

import com.foo.rest.examples.spring.openapi.v3.assertions.AssertionController
import com.foo.rest.examples.spring.openapi.v3.extraheader.ExtraHeaderController
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

class ExtraHeaderEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(ExtraHeaderController())
        }
    }


    @Test
    fun testRunEM() {
        runTestHandlingFlakyAndCompilation(
            "ExtraHeaderEM",
            "org.foo.ExtraHeaderEM",
            100
        ) { args: MutableList<String> ->

            //Does not seem needed for this example, as still using TT... does not look like for headers
            // there is a function returning a map of headers...
            //but maybe could be in other frameworks not dealing with JEE servlets
//            args.add("--extraHeader")
//            args.add("true")

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/extraheader", "OK")
        }
    }

}