package org.evomaster.e2etests.spring.openapi.v3.jsonpatchstringvalue

import com.foo.rest.examples.spring.openapi.v3.jsonpatchstringvalue.JsonPatchStringValueController
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

/**
 * E2E test: JSON Patch DTO with a PRIMITIVE (string) value.
 *
 * What is being tested:
 * When `dtoForRequestPayload` is enabled and the SUT has a JSON Patch endpoint whose resource
 * fields are all strings, EvoMaster must generate the DTO that represents the JSON Patch
 * operations (the shared `JsonPatchOperation` DTO, with the RFC 6902 fields op/path/from/value),
 * and each operation `value` must be a primitive string inlined as a literal (no nested value DTO).
 *
 * Why the assertion is unambiguous:
 * The SUT only exposes a GET (no request body -> no DTO) and a PATCH (json-patch). Therefore, if
 * EvoMaster generates any DTO at all, it can only be the JSON Patch one. Reflectively loading
 * `org.foo.dto.JsonPatchOperation` after the generated suite is compiled proves it was generated.
 *
 * This mirrors the reflective-assert approach of
 * [org.evomaster.e2etests.spring.openapi.v3.dtoreflectiveassert.DtoReflectiveAssertEMTest].
 */
class JsonPatchStringValueDtoEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(JsonPatchStringValueController())
        }
    }

    @Test
    fun testRunEM() {
        runTestHandlingFlakyAndCompilation(
            "JsonPatchStringValueDtoEM",
            "org.foo.JsonPatchStringValueDtoEM",
            100,
        ) { args: MutableList<String> ->

            setOption(args, "dtoForRequestPayload", "true")
            // This SUT intentionally has no authentication, and the PATCH returns 2xx, so the
            // security oracle would flag a fault 208 (Anonymous Modifications). It is irrelevant to
            // what we assert here (JSON Patch DTO generation), so we disable it to keep the suite clean.
            setOption(args, "security", "false")

            val solution = initAndRun(args)

            Assertions.assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.PATCH, 200, "/resource/{id}", "OK")
        }

        assertJsonPatchOperationDtoCreatedWithStringValue()
    }

    /**
     * The shared JsonPatchOperation DTO must exist with the RFC 6902 fields (op, path, from, value).
     * Because the resource only has string fields, `value` is a primitive: we prove it can hold a String.
     */
    private fun assertJsonPatchOperationDtoCreatedWithStringValue() {
        val (klass, instance) = initDtoClass("JsonPatchOperation")
        assertProperty(klass, instance, "op", "replace")
        assertProperty(klass, instance, "path", "/name")
        assertProperty(klass, instance, "from", "/description")
        // A JSON Patch value can be any JSON value; here it is a primitive string inlined as a literal.
        assertProperty(klass, instance, "value", "EvoMaster")
    }
}
