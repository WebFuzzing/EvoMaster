package org.evomaster.resource.rest.generator.implementation.java.entity

import org.evomaster.resource.rest.generator.implementation.java.JavaClass
import org.evomaster.resource.rest.generator.implementation.java.SpringAnnotation
import org.evomaster.resource.rest.generator.model.RepositoryClazz
import org.evomaster.resource.rest.generator.template.ClassType
import org.evomaster.resource.rest.generator.template.DeclarationScript
import org.evomaster.resource.rest.generator.template.MethodScript
import org.evomaster.resource.rest.generator.template.RegisterType

/**
 * created by manzh on 2019-08-15
 */
class JavaEntityRepository(specification: RepositoryClazz) : JavaClass<RepositoryClazz>(specification) {
    override fun getMethods(): List<MethodScript>  = listOf()

    override fun getImports(): List<String> = listOf(
            "org.springframework.data.repository.CrudRepository",
            "org.springframework.stereotype.Repository"
    )

    override fun getDeclaration(): List<DeclarationScript>  = listOf()

    override fun getImplementedInterface(): List<String> = listOf()

    override fun getTags(): List<String>  = listOf("@${SpringAnnotation.REPOSITORY.getText()}")

    override fun getName(): String = specification.name

    override fun getType(): ClassType = ClassType.INTERFACE

    override fun getSuperClazz(): List<String> {
        return listOf(
                "CrudRepository"
        )
    }

    override fun generateSuperClazz(types: RegisterType): List<String> {
        val referEntity = specification.entityType
        val type = specification.idType
        return listOf(
                "CrudRepository<$referEntity, ${types.getType(type)}>"
        )
    }
}