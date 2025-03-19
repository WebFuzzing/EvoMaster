package org.evomaster.e2etests.spring.openapi.v3.externalsref

import com.foo.rest.examples.spring.openapi.v3.externalsref.ExternalSrefController
import org.evomaster.core.problem.rest.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Paths

/**
 */
class ExternalSrefEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(ExternalSrefController())
        }
    }


    @Test
    fun testRunEM() {
        runTestHandlingFlakyAndCompilation(
                "ExternalSrefEM",
                20
        ) { args: MutableList<String> ->


            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/externalsref", "OK")
        }
    }
}