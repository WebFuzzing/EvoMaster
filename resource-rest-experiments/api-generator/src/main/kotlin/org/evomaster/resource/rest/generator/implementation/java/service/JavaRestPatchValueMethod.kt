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

    private val values = specification.entity.defaultProperties.map{it.name}
    private val idVar = "${specification.resourceOnPath}Id"
    private val valueVars = values.map { "${specification.resourceOnPath}$it" }

    override fun getParams(): Map<String, String> {
        return mapOf(
                idVar to specification.dto.idProperty.type).plus(valueVars.mapIndexed { index, d-> d to specification.entity.defaultProperties[index].type })
    }

    override fun getParamTag(): Map<String, String> {
        return mapOf(
                idVar to SpringAnnotation.PATH_VAR.getText(mapOf("name" to idVar)))
                .plus(valueVars
                        .map {valueVar-> valueVar to SpringAnnotation.REQUEST_PARAM.getText(mapOf("name" to valueVar)) }
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

        val withImpact = specification.entity.defaultProperties.any { it.impactful && it.branches > 1 }
        if (withImpact)
            content.add(initBranchesMessage())

        values.forEachIndexed { index, value ->

            val entityValue = specification.entity.defaultProperties.find {
                it.name == value
            }?: throw IllegalArgumentException("cannot find this property $value in entity")
            if(entityValue.impactful){
                content.add("$created.${entityValue.nameSetterName()}(${valueVars[index]});")
                if (entityValue.branches > 1)
                    content.add(
                            defaultBranches(
                                    type = entityValue.type,
                                    index = index,
                                    variableName =  valueVars[index],
                                    numOfBranches = entityValue.branches
                            )

                    )
            }
        }

        content.add(repositorySave(specification.entityRepository.name, created))
        if (!withImpact) content.add(returnStatus(200)) else content.add(returnStatus(200, msg = getBranchMsg()))
        return content
    }

    override fun getName(): String  = "update${FormatUtil.upperFirst(specification.name)}"

    override fun getBoundary(): Boundary = Boundary.PUBLIC
    override fun getReturn(): String? = "ResponseEntity"

    override fun getTags(): List<String> = listOf(
            "@${SpringAnnotation.REQUEST_MAPPING.getText(mapOf("value" to "/{$idVar}", "method" to "RequestMethod.PATCH", "produces" to "MediaType.APPLICATION_JSON"))}"
    )
}