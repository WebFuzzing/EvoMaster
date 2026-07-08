package org.evomaster.e2etests.spring.openapi.v3.jsonpatchstringvalue

import com.foo.rest.examples.spring.openapi.v3.jsonpatchstringvalue.JsonPatchStringValueController
import org.evomaster.client.java.instrumentation.shared.ClassName
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.Optional.ofNullable
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

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

    private fun initDtoClass(name: String): Pair<KClass<out Any>, Any> {
        val className = ClassName("org.foo.dto.$name")
        val klass = loadClass(className).kotlin
        Assertions.assertNotNull(klass)
        return Pair(klass, klass.createInstance())
    }

    private fun assertProperty(klass: KClass<out Any>, instance: Any, propertyName: String, propertyValue: Any?) {
        val property = klass.memberProperties
            .firstOrNull { it.name == propertyName } as? KMutableProperty1<Any, Any?>
        Assertions.assertNotNull(property)

        property?.let {
            it.isAccessible = true
            it.set(instance, ofNullable(propertyValue))
        }
        Assertions.assertEquals(ofNullable(propertyValue), property?.get(instance))
    }
}
