package org.evomaster.e2etests.spring.openapi.v3.uuid

import com.foo.rest.examples.spring.openapi.v3.assertions.AssertionController
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

class UuidEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(UuidController())
        }
    }


    @Test
    fun testRunEM() {
        runTestHandlingFlakyAndCompilation(
            "UuidEM",
            "org.foo.UuidEM",
            20
        ) { args: MutableList<String> ->

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/uuid/{a}", "OK")
        }
    }

}