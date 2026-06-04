package org.evomaster.e2etests.spring.rest.bb.jsonpatch

import com.foo.rest.examples.bb.jsonpatch.BBJsonPatchController
import org.evomaster.core.EMConfig
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.rest.bb.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class BBJsonPatchTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            val config = EMConfig()
            initClass(BBJsonPatchController(), config)
        }
    }

    @ParameterizedTest
    @EnumSource
    fun testBlackBoxOutput(outputFormat: OutputFormat) {
        executeAndEvaluateBBTest(
            outputFormat,
            "BBJsonPatchEM",
            1000,
            3,
            listOf(
                "PATCHED",
                "JSON_PATCH_ADD",
                "JSON_PATCH_REMOVE",
                "JSON_PATCH_REPLACE",
                "JSON_PATCH_MOVE",
                "JSON_PATCH_COPY",
                "JSON_PATCH_TEST",
                "JSON_PATCH_SEQUENCE"
            )
        ) { args: MutableList<String> ->

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
        }
    }
}