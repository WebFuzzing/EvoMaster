package org.evomaster.resource.rest.generator.implementation.java.service

import org.evomaster.resource.rest.generator.implementation.java.JavaMethod
import org.evomaster.resource.rest.generator.implementation.java.SpringAnnotation
import org.evomaster.resource.rest.generator.implementation.java.SpringRestAPI
import org.evomaster.resource.rest.generator.model.ServiceClazz
import org.evomaster.resource.rest.generator.template.Boundary

/**
 * created by manzh on 2019-08-15
 */
class JavaRestDeleteMethod(val specification: ServiceClazz) : JavaMethod(), SpringRestAPI {

    private val idVar = "${specification.resourceOnPath}Id"

    override fun getParams(): Map<String, String> {
        return mapOf(idVar to "${specification.dto.idProperty.type}")
    }

    override fun getParamTag(): Map<String, String> {
        return mapOf(idVar to SpringAnnotation.PATH_VAR.getText(mapOf("name" to idVar)))
    }

    override fun getBody(): List<String> {
        val content = mutableListOf<String>()
        content.add(assertExistence(specification.entityRepository.name, idVar))
        content.add(repositoryDeleteById(specification.entityRepository.name, idVar))
        content.add(returnStatus(200))
        return content
    }

    override fun getName(): String  = "delete${specification.entity.name}"

    override fun getReturn(): String? = "ResponseEntity"


    override fun getBoundary(): Boundary = Boundary.PUBLIC

    override fun getTags(): List<String> = listOf(
            "@${SpringAnnotation.REQUEST_MAPPING.getText(mapOf("value" to "/{$idVar}", "method" to "RequestMethod.DELETE", "produces" to "MediaType.APPLICATION_JSON"))}"
    )
}