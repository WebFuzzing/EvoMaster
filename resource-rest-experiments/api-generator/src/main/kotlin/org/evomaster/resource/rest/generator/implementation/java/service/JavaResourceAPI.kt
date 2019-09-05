package org.evomaster.resource.rest.generator.implementation.java.service

import org.evomaster.resource.rest.generator.model.RestMethod
import org.evomaster.resource.rest.generator.implementation.java.JavaClass
import org.evomaster.resource.rest.generator.implementation.java.SpringAnnotation
import org.evomaster.resource.rest.generator.model.ServiceClazz
import org.evomaster.resource.rest.generator.template.DeclarationScript
import org.evomaster.resource.rest.generator.template.MethodScript
import org.evomaster.resource.rest.generator.template.RegisterType

/**
 * created by manzh on 2019-08-15
 */
class JavaResourceAPI(specification: ServiceClazz) : JavaClass<ServiceClazz>(specification) {
    override fun getImports(): List<String> {
        return listOf(
                "${specification.entity.rootPackage}.*",
                "${specification.dto.rootPackage}.*",
                "org.springframework.beans.factory.annotation.Autowired",
                "org.springframework.http.ResponseEntity",
                "org.springframework.web.bind.annotation.*",
                "javax.ws.rs.core.MediaType",
                "java.util.*"
        )
    }

    override fun getMethods(): List<out MethodScript> {
        val methods = mutableListOf<MethodScript>()
        specification.restMethods.forEach { r->
            when(r){
                RestMethod.POST -> JavaRestPostMethod(this.specification).apply { methods.add(this) }
                RestMethod.GET_ALL -> JavaRestGetCollectionMethod(this.specification).apply { methods.add(this) }
                RestMethod.DELETE -> JavaRestDeleteMethod(this.specification).apply { methods.add(this) }
                RestMethod.GET_ID -> JavaRestGetByIdMethod(this.specification).apply { methods.add(this) }
                RestMethod.PATCH -> JavaRestPatchMethod(this.specification).apply { methods.add(this) }
                RestMethod.PATCH_VALUE -> JavaRestPatchValueMethod(this.specification).apply { methods.add(this) }
                RestMethod.PUT -> JavaRestPutMethod(this.specification).apply { methods.add(this) }
            }
        }
        return methods
    }

    override fun getDeclaration(): List<out DeclarationScript> {
        val repositories = mutableListOf<JavaDeclarationRepository>()
        repositories.add(JavaDeclarationRepository(specification.entityRepository))
        specification.obviousReferEntityRepositories.map { p->
            repositories.add(JavaDeclarationRepository(p.value))
        }
        specification.hideReferEntityRepositories.map { p->
            repositories.add(JavaDeclarationRepository(p.value))
        }
        return repositories
    }

    override fun getImplementedInterface(): List<String> = listOf()

    override fun getSuperClazz(): List<String> = listOf()

    override fun generateConstructors(types: RegisterType) : List<String> = listOf()

    override fun getTags(): List<String> {
        return listOf(SpringAnnotation.REST_CONTROLLER.getText(), SpringAnnotation.REQUEST_MAPPING.getText(mapOf("path" to "/api/${specification.resourceOnPath}"))).map { "@$it" }
    }

    override fun getName(): String = specification.name
}