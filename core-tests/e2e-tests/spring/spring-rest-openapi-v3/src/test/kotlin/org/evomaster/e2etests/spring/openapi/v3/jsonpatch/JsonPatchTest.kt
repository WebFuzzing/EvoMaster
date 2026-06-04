package org.evomaster.e2etests.spring.openapi.v3.jsonpatch

import com.foo.rest.examples.spring.openapi.v3.jsonpatch.JsonPatchController
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class JsonPatchTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(JsonPatchController())
        }
    }

    @Test
    fun testRunEM() {
        runTestHandlingFlakyAndCompilation(
            "JsonPatchEM",
            "org.foo.JsonPatchEM",
            2000,
            true,
            { args: MutableList<String> ->

                val solution = initAndRun(args)

                assertTrue(solution.individuals.size >= 1)

                assertHasAtLeastOne(solution, HttpVerb.PATCH, 200, "/pets/{id}", "patched")

                assertHasAtLeastOne(solution, HttpVerb.PATCH, 200, "/pets/{id}/add", "add patched")
                assertHasAtLeastOne(solution, HttpVerb.PATCH, 400, "/pets/{id}/add", null)

                assertHasAtLeastOne(solution, HttpVerb.PATCH, 200, "/pets/{id}/remove", "remove patched")
                assertHasAtLeastOne(solution, HttpVerb.PATCH, 400, "/pets/{id}/remove", null)

                assertHasAtLeastOne(solution, HttpVerb.PATCH, 200, "/pets/{id}/replace", "replace patched")
                assertHasAtLeastOne(solution, HttpVerb.PATCH, 400, "/pets/{id}/replace", null)

                assertHasAtLeastOne(solution, HttpVerb.PATCH, 200, "/pets/{id}/move", "move patched")
                assertHasAtLeastOne(solution, HttpVerb.PATCH, 400, "/pets/{id}/move", null)

                assertHasAtLeastOne(solution, HttpVerb.PATCH, 200, "/pets/{id}/copy", "copy patched")
                assertHasAtLeastOne(solution, HttpVerb.PATCH, 400, "/pets/{id}/copy", null)

                assertHasAtLeastOne(solution, HttpVerb.PATCH, 200, "/pets/{id}/test", "test patched")
                assertHasAtLeastOne(solution, HttpVerb.PATCH, 400, "/pets/{id}/test", null)

                assertHasAtLeastOne(solution, HttpVerb.PATCH, 200, "/pets/{id}/sequence", "sequence patched")
                assertHasAtLeastOne(solution, HttpVerb.PATCH, 400, "/pets/{id}/sequence", null)
            },
            3,
        )
    }
}