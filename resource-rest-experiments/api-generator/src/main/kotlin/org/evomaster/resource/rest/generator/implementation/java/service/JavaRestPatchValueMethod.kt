package org.evomaster.resource.rest.generator.implementation.java.service

import org.evomaster.resource.rest.generator.FormatUtil
import org.evomaster.resource.rest.generator.model.CommonTypes
import org.evomaster.resource.rest.generator.implementation.java.JavaMethod
import org.evomaster.resource.rest.generator.implementation.java.SpringAnnotation
import org.evomaster.resource.rest.generator.implementation.java.SpringRestAPI
import org.evomaster.resource.rest.generator.model.ServiceClazz
import org.evomaster.resource.rest.generator.template.Boundary

/**
 * created by manzh on 2019-08-15
 */
class JavaRestPatchValueMethod(val specification: ServiceClazz) : JavaMethod(), SpringRestAPI {

    private val value = "value"
    private val idVar = "${specification.resourceOnPath}Id"
    private val valueVar = "${specification.resourceOnPath}$value"

    override fun getParams(): Map<String, String> {
        val type = CommonTypes.INT.toString()
        return mapOf(
                idVar to specification.dto.idProperty.type,
                valueVar to type)
    }

    override fun getParamTag(): Map<String, String> {
        return mapOf(
                idVar to SpringAnnotation.PATH_VAR.getText(mapOf("name" to idVar)),
                valueVar to SpringAnnotation.REQUEST_PARAM.getText(mapOf("name" to valueVar))
                )
    }

    override fun getBody(): List<String> {
        val content = mutableListOf<String>()
        val created = "node"
        content.add(findEntityByIdAndAssigned(
                specification.entityRepository.name,
                idVar,
                created,
                specification.entity.name
        ))

        val entityValue = specification.entity.defaultProperties.find {
            it.name == value
        }?: throw IllegalArgumentException("cannot find this property $value in entity")


        content.add("$created.${entityValue.nameSetterName()}($valueVar);")

        content.add(repositorySave(specification.entityRepository.name, created))
        content.add(returnStatus(200))
        return content
    }

    override fun getName(): String  = "update${FormatUtil.upperFirst(specification.name)}"

    override fun getBoundary(): Boundary = Boundary.PUBLIC
    override fun getReturn(): String? = "ResponseEntity"

    override fun getTags(): List<String> = listOf(
            "@${SpringAnnotation.REQUEST_MAPPING.getText(mapOf("value" to "/{$idVar}", "method" to "RequestMethod.PATCH", "produces" to "MediaType.APPLICATION_JSON"))}"
    )
}