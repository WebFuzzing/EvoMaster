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
class JavaRestGetCollectionMethod(val specification: ServiceClazz) : JavaMethod(), SpringRestAPI {


    override fun getParams(): Map<String, String> = mapOf()

    override fun getParamTag(): Map<String, String> = mapOf()

    override fun getReturn(): String? = "ResponseEntity<List<${specification.dto.name}>>"

    override fun getBody(): List<String> {
        val content = mutableListOf<String>()
        val allDtos = "allDtos"
        content.add(findAllEntitiesAndConvertToDto(specification.entityRepository.name, specification.entity.name, allDtos, JavaE2DMethod(specification.entity).getInvocation(null),specification.dto.name))
        content.add(returnWithContent(allDtos))
        return content
    }

    override fun getName(): String  = "getAll${specification.entity.name}"

    override fun getBoundary(): Boundary = Boundary.PUBLIC

    override fun getTags(): List<String> = listOf(
            "@${SpringAnnotation.REQUEST_MAPPING.getText(mapOf("value" to "", "method" to "RequestMethod.GET", "produces" to "MediaType.APPLICATION_JSON"))}"
    )
}