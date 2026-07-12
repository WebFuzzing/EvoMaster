package org.evomaster.e2etests.spring.openapi.v3

import org.evomaster.client.java.controller.InstrumentedSutStarter
import org.evomaster.client.java.instrumentation.InputProperties
import org.evomaster.client.java.instrumentation.InstrumentingAgent
import org.evomaster.client.java.instrumentation.shared.ClassName
import org.evomaster.e2etests.utils.RestTestBase
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import java.util.Optional.ofNullable
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

/**
 * Created by arcuri82 on 03-Mar-20.
 */
abstract class SpringTestBase : RestTestBase(){

    companion object {
        @BeforeAll
        @JvmStatic
        fun initAgent() {
            /*
                needed because kotlin.jvm.internal.Intrinsics gets loaded in
                TaintKotlinEqualController before agent is initialized
             */
            System.setProperty(InputProperties.REPLACEMENT_CATEGORIES, "BASE,SQL,EXT_0")
            InstrumentedSutStarter.loadAgent()
            InstrumentingAgent.changePackagesToInstrument("com.foo.")
        }
    }

    protected fun initDtoClass(name: String): Pair<KClass<out Any>, Any> {
        val className = ClassName("org.foo.dto.$name")
        val klass = loadClass(className).kotlin
        Assertions.assertNotNull(klass)
        return Pair(klass, klass.createInstance())
    }

    protected fun assertProperty(klass: KClass<out Any>, instance: Any, propertyName: String, propertyValue: Any?) {
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
