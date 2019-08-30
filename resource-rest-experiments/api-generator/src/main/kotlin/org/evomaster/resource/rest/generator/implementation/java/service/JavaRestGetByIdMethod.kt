package org.evomaster.resource.rest.generator.implementation.java.service

import org.evomaster.resource.rest.generator.implementation.java.JavaMethod
import org.evomaster.resource.rest.generator.implementation.java.SpringAnnotation
import org.evomaster.resource.rest.generator.implementation.java.SpringRestAPI
import org.evomaster.resource.rest.generator.implementation.java.entity.JavaE2DMethod
import org.evomaster.resource.rest.generator.model.ServiceClazz
import org.evomaster.resource.rest.generator.template.Boundary

/**
 * created by manzh on 2019-08-15
 */
class JavaRestGetByIdMethod(val specification: ServiceClazz) : JavaMethod(), SpringRestAPI {

    private val idVar = "${specification.resourceOnPath}Id"

    override fun getParams(): Map<String, String> {
        return mapOf(idVar to "${specification.dto.idProperty.type}")
    }

    override fun getParamTag(): Map<String, String> {
        return mapOf(idVar to SpringAnnotation.PATH_VAR.getText(mapOf("name" to idVar)))
    }

    override fun getBody(): List<String> {
        val content = mutableListOf<String>()
        var dto = "dto"
        content.add(
                findEntityByIdAndConvertToDto(specification.entityRepository.name, idVar, specification.entity.name, dto, specification.dto.name, JavaE2DMethod(specification.entity).getInvocation(null))
        )
        content.add(returnWithContent(dto))
        return content
    }

    override fun getName(): String  = "get${specification.entity.name}"

    override fun getReturn(): String? = "ResponseEntity<${specification.dto.name}>"

    override fun getBoundary(): Boundary = Boundary.PUBLIC

    override fun getTags(): List<String> = listOf(
            "@${SpringAnnotation.REQUEST_MAPPING.getText(mapOf("value" to "/{$idVar}", "method" to "RequestMethod.GET", "produces" to "MediaType.APPLICATION_JSON"))}"
    )
}