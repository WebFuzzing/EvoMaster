package org.evomaster.e2etests.spring.openapi.v3.jsonpatchobjectvalue

import com.foo.rest.examples.spring.openapi.v3.jsonpatchobjectvalue.JsonPatchObjectValueController
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

/**
 * E2E test: JSON Patch DTO with a NON-PRIMITIVE value.
 *
 * What is being tested:
 * When `dtoForRequestPayload` is enabled and the SUT has a JSON Patch endpoint whose resource
 * exposes an array-of-objects field, EvoMaster must generate:
 *  1. the DTO that represents the JSON Patch operations (the shared `JsonPatchOperation` DTO), and
 *  2. the nested DTO for the NON-PRIMITIVE `value` of those operations (the `Item` object DTO).
 *
 * Why the assertion is unambiguous:
 * The SUT only exposes a GET (no request body -> no DTO) and a PATCH (json-patch). Therefore the
 * only DTOs EvoMaster can generate are the JSON Patch ones. Reflectively loading
 * `org.foo.dto.JsonPatchOperation` and `org.foo.dto.Item` after the generated suite is compiled
 * proves that both were generated, i.e. that the value DTO of the JSON Patch operations exists.
 *
 * This mirrors the reflective-assert approach of
 * [org.evomaster.e2etests.spring.openapi.v3.dtoreflectiveassert.DtoReflectiveAssertEMTest].
 */
class JsonPatchObjectValueDtoEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(JsonPatchObjectValueController())
        }
    }

    @Test
    fun testRunEM() {
        runTestHandlingFlakyAndCompilation(
            "JsonPatchObjectValueDtoEM",
            "org.foo.JsonPatchObjectValueDtoEM",
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

        assertJsonPatchOperationDtoCreated()
        assertNonPrimitiveValueDtoCreated()
    }

    /**
     * The shared JsonPatchOperation DTO must exist with the RFC 6902 fields (op, path, from, value).
     */
    private fun assertJsonPatchOperationDtoCreated() {
        val (klass, instance) = initDtoClass("JsonPatchOperation")
        assertProperty(klass, instance, "op", "add")
        assertProperty(klass, instance, "path", "/items")
        assertProperty(klass, instance, "from", "/items")
    }

    /**
     * The nested DTO for the non-primitive `value` of the operations must exist. Its name comes
     * from the OpenAPI component `Item`, with fields label (String) and quantity (Int).
     */
    private fun assertNonPrimitiveValueDtoCreated() {
        val (klass, instance) = initDtoClass("Item")
        assertProperty(klass, instance, "label", "a label")
        assertProperty(klass, instance, "quantity", 7)
    }
}
