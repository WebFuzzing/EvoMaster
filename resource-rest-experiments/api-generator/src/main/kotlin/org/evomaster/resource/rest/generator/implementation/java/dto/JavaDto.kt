package org.evomaster.resource.rest.generator.implementation.java.dto

import org.evomaster.resource.rest.generator.implementation.java.JavaClass
import org.evomaster.resource.rest.generator.implementation.java.SwaggerAnnotation
import org.evomaster.resource.rest.generator.model.DtoClazz
import org.evomaster.resource.rest.generator.template.DeclarationScript
import org.evomaster.resource.rest.generator.template.MethodScript

/**
 * created by manzh on 2019-08-15
 */
class JavaDto (specification: DtoClazz): JavaClass<DtoClazz>(specification) {

    override fun getMethods(): List<MethodScript> = listOf()

    override fun getTags(): List<String> = listOf("@${SwaggerAnnotation.API_MODEL.getText()}")

    override fun getDeclaration(): List<out DeclarationScript> {
        val declarations = mutableListOf<DeclarationScript>()
        if (!specification.idFromSuperClazz) declarations.add(JavaDeclarationDto(specification.idProperty))
        specification.defaultProperties.plus(specification.referToOthers).plus(specification.ownOthers).plus(specification.ownOthersProperties.flatten()).forEach { p->
            declarations.add(JavaDeclarationDto(p))
        }
        return declarations
    }

    override fun getFileName(): String = getName()

    override fun getImports(): List<String> = SwaggerAnnotation.requiredPackages().asList()

    override fun getImplementedInterface(): List<String> = listOf()

    override fun getSuperClazz(): List<String> = listOf()

    override fun getName(): String = specification.name
}