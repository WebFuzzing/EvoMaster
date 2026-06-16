package org.evomaster.e2etests.spring.rest.bb.httppatch

import com.foo.rest.examples.bb.httppatch.HttpPatchController
import org.evomaster.core.EMConfig
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.rest.bb.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

/**
 * End-to-end test for JSON Patch (RFC 6902) against a realistic, vendored API
 * (a slice of https://github.com/cassiomolin/http-patch-spring, exposing PATCH /contacts/{id}
 * with media type application/json-patch+json).
 *
 * Unlike BBJsonPatchTest -- which uses a synthetic SUT that only checks the *presence* of each
 * operation and never mutates state -- here the patch is actually applied to a real Contact
 * resource. This exercises:
 *   - resolving the resource schema from the sibling GET /contacts/{id} response, so generated
 *     paths/values reference real fields (/name, /notes, /favorite, /work/title, ...);
 *   - that EvoMaster covers all six operations (add/remove/replace/move/copy/test);
 *   - that a patch which would leave the resource invalid (e.g. removing the required name) is
 *     detected and rejected (422) without corrupting state -> JSONPATCH_INVALID_RESOURCE.
 */
class HttpPatchTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            val config = EMConfig()
            initClass(HttpPatchController(), config)
        }
    }

    @ParameterizedTest
    @EnumSource
    fun testBlackBoxOutput(outputFormat: OutputFormat) {
        executeAndEvaluateBBTest(
            outputFormat,
            "HttpPatchEM",
            1000,
            3,
            listOf(
                "JSONPATCH_OP_ADD",
                "JSONPATCH_OP_REMOVE",
                "JSONPATCH_OP_REPLACE",
                "JSONPATCH_OP_MOVE",
                "JSONPATCH_OP_COPY",
                "JSONPATCH_OP_TEST",
                "JSONPATCH_APPLIED_OK",
                "JSONPATCH_INVALID_RESOURCE",
                "JSONPATCH_CONFLICT"
            )
        ) { args: MutableList<String> ->

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)

            // A patch that applies cleanly and persists.
            assertHasAtLeastOne(solution, HttpVerb.PATCH, 200, "/contacts/{id}", null)
            // A patch that would leave the resource invalid -> rejected, state untouched.
            assertHasAtLeastOne(solution, HttpVerb.PATCH, 422, "/contacts/{id}", null)
            // A structurally inapplicable patch (bad path / failed test op) -> conflict.
            assertHasAtLeastOne(solution, HttpVerb.PATCH, 409, "/contacts/{id}", null)
        }
    }
}
