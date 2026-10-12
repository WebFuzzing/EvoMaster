package org.evomaster.e2etests.spring.openapi.v3.zerochunk

import com.foo.rest.examples.spring.openapi.v3.zerochunk.ZeroChunkController
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class ZeroChunkEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(ZeroChunkController())
        }
    }

    // unclosed empty-body responses leak pooled connections, and the search then blocks forever
    @Test
    fun testRunEM() {
        runTestHandlingFlaky(
            "ZeroChunkEM",
            "org.foo.ZeroChunkEM",
            100,
            true,
            { args: List<String> ->

                val solution = initAndRun(args)

                assertTrue(solution.individuals.size >= 1)
                assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/zerochunk/empty", null)
                assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/zerochunk/a", "A")
                assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/zerochunk/b", "B")
            },
            1
        )
    }
}
