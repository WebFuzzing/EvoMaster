package org.evomaster.e2etests.spring.openapi.v3.uri

import com.foo.rest.examples.spring.openapi.v3.assertions.AssertionController
import com.foo.rest.examples.spring.openapi.v3.uri.UriController
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

class UriEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(UriController())
        }
    }


    @Test
    fun testRunEM() {
        runTestHandlingFlakyAndCompilation(
            "UriEM",
            "org.foo.UriEM",
            100
        ) { args: MutableList<String> ->

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/uri/http", "OK")
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/uri/data", "OK")
        }
    }

}