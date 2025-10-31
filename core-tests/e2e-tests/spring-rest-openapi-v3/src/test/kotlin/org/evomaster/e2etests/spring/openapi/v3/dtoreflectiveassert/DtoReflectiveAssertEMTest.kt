package org.evomaster.e2etests.spring.openapi.v3.dtoreflectiveassert

import com.foo.rest.examples.spring.openapi.v3.dtoreflectiveassert.DtoReflectiveAssertController
import org.evomaster.client.java.instrumentation.shared.ClassName
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

class DtoReflectiveAssertEMTest: SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(DtoReflectiveAssertController())
        }
    }

    @Test
    fun testRunEM() {
        runTestHandlingFlakyAndCompilation(
            "ReflectiveAssertEM",
            "org.foo.ReflectiveAssertEM",
            100,
        ) { args: MutableList<String> ->

            setOption(args,"dtoForRequestPayload","true")

            val solution = initAndRun(args)

            Assertions.assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/allof", "OK")
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/primitiveTypes", "OK")
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/parent", "OK")
        }

        assertPrimitiveTypeDtoCreated()
        assertParentAndChildDtosCreated()
        assertAllOfDtoCreated()
        // TODO: Restore when support for ChoiceGene has been added
//        assertAnyOfDtoCreated()
//        assertOneOfDtoCreated()
    }

    private fun assertPrimitiveTypeDtoCreated() {
        val (klass, instance) = initDtoClass("POST__primitiveTypes")
        assertProperty(klass, instance, "aString", "hello")
        assertProperty(klass, instance, "aRegex", "^[a-zA-Z0-9]+\\$")
        assertProperty(klass, instance, "aDate", "2025-08-03")
        assertProperty(klass, instance, "aTime", "14:30:00")
        assertProperty(klass, instance, "aDateTime", "2025-08-03T14:30:00Z")
        assertProperty(klass, instance, "anInteger", -3)
        assertProperty(klass, instance, "aLong", 9223372036854775807L)
        assertProperty(klass, instance, "aDouble", 3.1415)
        assertProperty(klass, instance, "aFloat", 2.718f)
        assertProperty(klass, instance, "aBoolean", true)
        assertProperty(klass, instance, "aNullableString", null)
    }

    private fun assertParentAndChildDtosCreated() {
        val (parentKlass, parentInstance) = initDtoClass("ParentSchema")
        val (childKass, childInstance) = initDtoClass("ChildSchema")
        assertProperty(childKass, childInstance, "name", "Philip")
        assertProperty(childKass, childInstance, "age", 31)
        assertProperty(parentKlass, parentInstance, "label", "EM_TEST")
        assertProperty(parentKlass, parentInstance, "child", childInstance)
    }

    private fun assertAllOfDtoCreated() {
        val (klass, instance) = initDtoClass("POST__allof")
        assertProperty(klass, instance, "name", "Philip")
        assertProperty(klass, instance, "age", 31)
    }

    private fun assertAnyOfDtoCreated() {
        val (klass, instance) = initDtoClass("POST__anyof")
        assertProperty(klass, instance, "email", "evomaster@webfuzzing.com")
        assertProperty(klass, instance, "phone", "+54123151")
    }

    private fun assertOneOfDtoCreated() {
        val (klass, instance) = initDtoClass("POST__oneof")
        assertProperty(klass, instance, "cat", "Tom")
        assertProperty(klass, instance, "mouse", "Jerry")
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
            it.set(instance, propertyValue) // Set the value
        }
        Assertions.assertEquals(propertyValue, property?.get(instance))
    }

}
