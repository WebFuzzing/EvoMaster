package org.evomaster.resource.rest.generator.implementation.java.entity

import org.evomaster.resource.rest.generator.implementation.java.JavaClass
import org.evomaster.resource.rest.generator.implementation.java.JavaGetterMethod
import org.evomaster.resource.rest.generator.implementation.java.JavaSetterMethod
import org.evomaster.resource.rest.generator.implementation.java.JavaxAnnotation
import org.evomaster.resource.rest.generator.model.EntityClazz
import org.evomaster.resource.rest.generator.template.DeclarationScript
import org.evomaster.resource.rest.generator.template.MethodScript

/**
 * created by manzh on 2019-08-15
 */
class JavaEntity (specification: EntityClazz): JavaClass<EntityClazz>(specification) {

    override fun getMethods(): List<out MethodScript> {
        val methods = mutableListOf<MethodScript>()
        if (!specification.idFromSuperClazz){
            methods.add(JavaSetterMethod(specification.idProperty))
            methods.add(JavaGetterMethod(specification.idProperty))
        }
        specification.defaultProperties.plus(specification.referToOthers).plus(specification.ownOthers).forEach{ p->
            methods.add(JavaSetterMethod(p))
            methods.add(JavaGetterMethod(p))
        }
        methods.add(JavaE2DMethod(specification))
        return methods
    }

    override fun getTags(): List<String> {
        val tags = mutableListOf<String>()

        if (this.specification.isATable) {
            tags.add(JavaxAnnotation.ENTITY.getText())
            tags.add(JavaxAnnotation.Table.getText(mapOf("name" to specification.resourceName)))
        }else tags.add(JavaxAnnotation.MAPPED_SUPPERCLASS.getText())

        return tags.map { "@$it" }.toList()
    }

    override fun getFileName(): String  = getName()

    override fun getImports(): List<String> = JavaxAnnotation.requiredPackages().asList().plus("${specification.dto.rootPackage}.*")

    override fun getImplementedInterface(): List<String> = listOf()

    override fun getSuperClazz(): List<String> = listOf()

    override fun getName(): String = specification.name

    override fun getDeclaration(): List<out DeclarationScript> {
        val declarations = mutableListOf<DeclarationScript>()
        if (!specification.idFromSuperClazz) declarations.add(JavaDeclarationEntity(specification.idProperty))
        specification.defaultProperties.plus(specification.referToOthers).plus(specification.ownOthers).forEach { p->
            declarations.add(JavaDeclarationEntity(p))
        }
        return declarations
    }
}