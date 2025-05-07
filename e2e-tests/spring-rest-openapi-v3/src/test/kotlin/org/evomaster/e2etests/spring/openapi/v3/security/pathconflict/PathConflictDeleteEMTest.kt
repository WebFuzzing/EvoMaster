package org.evomaster.e2etests.spring.openapi.v3.security.pathconflict

import com.foo.rest.examples.spring.openapi.v3.security.pathconflict.PathConflictDeleteController
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class PathConflictDeleteEMTest : SpringTestBase(){

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(PathConflictDeleteController())
        }
    }


    @Test
    fun testRunEM() {

        runTestHandlingFlakyAndCompilation(
                "PathConflictDeleteEM",
                200
        ) { args: MutableList<String> ->

            setOption(args, "security", "true")
            setOption(args, "schemaOracles", "false")

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)

            assertHasAtLeastOne(solution, HttpVerb.POST, 201, "/api/articles", null)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/articles/{id}", null)
            assertHasAtLeastOne(solution, HttpVerb.GET, 401, "/api/articles/{id}", null)
            assertHasAtLeastOne(solution, HttpVerb.POST, 201, "/api/articles/{id}/comments", null)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/articles/{id}/comments/{commentId}", null)
            assertHasAtLeastOne(solution, HttpVerb.GET, 401, "/api/articles/{id}/comments/{commentId}", null)

        }
    }
}