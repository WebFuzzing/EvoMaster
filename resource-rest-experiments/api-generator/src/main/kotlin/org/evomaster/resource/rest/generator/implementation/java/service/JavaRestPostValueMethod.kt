package org.evomaster.resource.rest.generator.implementation.java.service

import org.evomaster.resource.rest.generator.FormatUtil
import org.evomaster.resource.rest.generator.implementation.java.JavaMethod
import org.evomaster.resource.rest.generator.implementation.java.JavaUtils
import org.evomaster.resource.rest.generator.implementation.java.SpringAnnotation
import org.evomaster.resource.rest.generator.implementation.java.SpringRestAPI
import org.evomaster.resource.rest.generator.model.*
import org.evomaster.resource.rest.generator.template.Boundary

/**
 * created by manzh on 2019-08-15
 */
class JavaRestPostValueMethod(specification: ServiceClazz, method : RestMethod) : JavaRestMethod(specification, method){

    private val values = specification.entity.defaultProperties.map{it.name}
    private val idVar = "${specification.resourceOnPath}Id"
    private val valueVars = values.map { "${specification.resourceOnPath}$it" }

    private val referVars = specification.dto.referToOthers.map { Pair(it.name, it.type) }
    private val ownedVars = specification.dto.ownOthers.map { Pair(it.name, it.type)}
    private val ownedPVars = specification.dto.ownOthersProperties.map { s-> s.map { Pair(it.name, it.type) } }
    override fun getParams(): Map<String, String> {
        return mapOf(idVar to specification.dto.idProperty.type)
                .plus(valueVars.mapIndexed { index, d-> d to specification.entity.defaultProperties[index].type })
                .plus(referVars.toMap())
                .plus(ownedVars.toMap())
                .plus(ownedPVars.flatten().toMap())
    }

    override fun getParamTag(): Map<String, String> {
        return mapOf(
                idVar to SpringAnnotation.PATH_VAR.getText(mapOf("name" to idVar)))
                .plus(valueVars.plus(referVars.plus(ownedVars).toMap().keys).plus(ownedPVars.flatMap { it.map { s->s.first } })
                        .map {valueVar-> valueVar to SpringAnnotation.REQUEST_PARAM.getText(mapOf("name" to valueVar)) }
                )

    }

    override fun getBody(): List<String> {
        val content = mutableListOf<String>()

        //check if the id exists
        content.add(assertNonExistence(specification.entityRepository.name, idVar))

        val created = "node"
        content.add(formatInstanceClassAndAssigned(specification.entity.name, created, listOf()))

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

        //refer to related entity
        content.add("$created.${specification.entity.idProperty.nameSetterName()}($idVar);")
        if(referVars.isNotEmpty())
            content.add(JavaUtils.getSingleComment("refer to related entity"))
        (0 until specification.dto.referToOthers.size).forEach { index->
            val entityProperty = specification.entity.referToOthers[index]

            val found = "referVarTo${entityProperty.type}"
            val refer = referVars[index].first

            content.add(findEntityByIdAndAssigned(
                    specification.obviousReferEntityRepositories.getValue(entityProperty.type).name,
                    refer,
                    found,
                    entityProperty.type
            ))

            content.add("$created.${entityProperty.nameSetterName()}($found);")
        }

        //created owned entity by dto
        val ownedId = specification.dto.ownOthers
        val ownedProperties = specification.dto.ownOthersProperties
        val ownedTypes = specification.dto.ownOthersTypes
        if(ownedId.isNotEmpty()){
            content.add(JavaUtils.getSingleComment("create owned entity"))
            (0 until ownedId.size).forEach { index->
                val createdDtoVar = "ownedDto$index" //dto
                content.add(formatInstanceClassAndAssigned( ownedTypes[index], createdDtoVar, listOf()))
                val varsOnParams = ownedPVars[index]
                val opid = ownedId[index] as? ResNodeTypedPropertySpecification?:throw IllegalArgumentException("wrong property spec")
                content.add("$createdDtoVar.${opid.itsIdProperty.name}=${ownedVars[index].first};")

                ownedProperties[index].forEachIndexed { pi, op->
                    val opp = op as? ResNodeTypedPropertySpecification?:throw IllegalArgumentException("wrong property spec")
                    content.add("$createdDtoVar.${opp.itsIdProperty.name}=${varsOnParams[pi].first};")
                }
                val apiVar = specification.ownedResourceService[ownedTypes[index]] as? ResServiceTypedPropertySpecification
                        ?:throw IllegalArgumentException("cannot find service to create the owned entity")

                content.add("${apiVar.name}.${Utils.generateRestMethodName(RestMethod.POST, apiVar.resourceName)}($createdDtoVar);")
                val entityProperty = specification.entity.ownOthers[index]
                val found = "ownedEntity${entityProperty.type}"
                val id = ownedVars[index].first
                content.add(findEntityByIdAndAssigned(
                        specification.ownedEntityRepositories.getValue(entityProperty.type).name,
                        id,
                        found,
                        entityProperty.type
                ))
                content.add("$created.${entityProperty.nameSetterName()}($found);")
            }
        }

        content.add(JavaUtils.getSingleComment("save the entity"))
        content.add(repositorySave(specification.entityRepository.name, created))
        if (!withImpact) content.add(returnStatus(200)) else content.add(returnStatus(200, msg = getBranchMsg()))
        return content
    }

    override fun getBoundary(): Boundary = Boundary.PUBLIC
    override fun getReturn(): String? = "ResponseEntity"

    override fun getTags(): List<String> = listOf(
            "@${SpringAnnotation.REQUEST_MAPPING.getText(mapOf("value" to "/{$idVar}", "method" to "RequestMethod.POST", "produces" to "MediaType.APPLICATION_JSON"))}"
    )
}