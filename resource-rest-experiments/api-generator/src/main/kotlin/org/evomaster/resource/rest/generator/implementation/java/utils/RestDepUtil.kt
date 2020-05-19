package org.evomaster.resource.rest.generator.implementation.java.utils

import org.evomaster.resource.rest.generator.implementation.java.JavaClass
import org.evomaster.resource.rest.generator.model.ServiceUtilClazz
import org.evomaster.resource.rest.generator.template.DeclarationScript
import org.evomaster.resource.rest.generator.template.MethodScript
import org.evomaster.resource.rest.generator.template.RegisterType

/**
 * created by manzh on 2019-12-20
 */
class RestDepUtil (specification : ServiceUtilClazz) : JavaClass<ServiceUtilClazz>(specification) {
    override fun getTags(): List<String> = listOf()

    override fun getImports(): List<String> = listOf(
            "java.util.Arrays"
    )

    override fun getMethods(): List<MethodScript> = listOf(
            AvgMethod(),
            MediumMethod()
    )

    override fun generateConstructors(types: RegisterType): List<String> = listOf()

    override fun getDeclaration(): List<DeclarationScript>  = listOf()

    override fun getImplementedInterface(): List<String> = listOf()

    override fun getSuperClazz(): List<String> = listOf()

    override fun getName(): String = specification.name
}