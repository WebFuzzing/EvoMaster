package org.evomaster.e2etests.spring.openapi.v3.jsonpatchobjectvalue

import com.foo.rest.examples.spring.openapi.v3.jsonpatchobjectvalue.JsonPatchObjectValueController
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
