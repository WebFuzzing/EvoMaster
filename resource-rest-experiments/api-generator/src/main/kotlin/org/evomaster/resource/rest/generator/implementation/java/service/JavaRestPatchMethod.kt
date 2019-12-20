package org.evomaster.resource.rest.generator.implementation.java.service

import org.evomaster.resource.rest.generator.FormatUtil
import org.evomaster.resource.rest.generator.implementation.java.JavaMethod
import org.evomaster.resource.rest.generator.implementation.java.SpringAnnotation
import org.evomaster.resource.rest.generator.implementation.java.SpringRestAPI
import org.evomaster.resource.rest.generator.model.RestMethod
import org.evomaster.resource.rest.generator.model.ServiceClazz
import org.evomaster.resource.rest.generator.template.Boundary

/**
 * created by manzh on 2019-08-15
 */
class JavaRestPatchMethod(specification: ServiceClazz, method : RestMethod) : JavaRestMethod(specification, method){

    private val dtoVar = FormatUtil.lowerFirst(specification.dto.name)

    override fun getParams(): Map<String, String> {
        return mapOf(dtoVar to "${specification.dto.name}")
    }

    override fun getParamTag(): Map<String, String> {
        return mapOf(dtoVar to SpringAnnotation.REQUEST_BODY.getText())
    }

    override fun getBody(): List<String> {
        val content = mutableListOf<String>()

        val entityId = "$dtoVar.${specification.dto.idProperty.name}"

        val created = "node"
        //check if the id exists
        content.add(findEntityByIdAndAssigned(
                specification.entityRepository.name,
                entityId,
                created,
                specification.entity.name
        ))

        //set property regarding dto
        content.add("$created.${specification.entity.idProperty.nameSetterName()}($entityId);")

        val withImpact = specification.entity.defaultProperties.any { it.impactful && it.branches > 1 }
        if (withImpact)
            content.add(initBranchesMessage())

        (0 until specification.entity.defaultProperties.size).forEachIndexed { index, i->
            val property = specification.entity.defaultProperties[i]
            if (property.impactful){
                val variableName = "$dtoVar.${specification.dto.defaultProperties[i].name}"
                content.add("$created.${specification.entity.defaultProperties[i].nameSetterName()}($variableName);")
                if (property.branches > 1){
                    content.add(
                            defaultBranches(
                                    type = property.type,
                                    index = index,
                                    numOfBranches = property.branches,
                                    variableName = variableName
                            )
                    )
                }
            }
        }

        //check if the specified reference exists, if reference exists, then set it to the created
        val vars = "referVarTo"
        assert( specification.dto.referToOthers.size == specification.entity.referToOthers.size )
        (0 until specification.dto.referToOthers.size).forEach { index->
            val entityProperty = specification.entity.referToOthers[index]
            val dtoProperty = specification.dto.referToOthers[index]

            val found = "$vars${entityProperty.type}"
            val refer = "$dtoVar.${dtoProperty.name}"

            content.add(findEntityByIdAndAssigned(
                    specification.obviousReferEntityRepositories.getValue(entityProperty.type).name,
                    refer,
                    found,
                    entityProperty.type
            ))

            content.add("$created.${entityProperty.nameSetterName()}($found);")
        }

        //TODO regarding hide reference

        content.add(repositorySave(specification.entityRepository.name, created))
        if (!withImpact) content.add(returnStatus()) else content.add(returnStatus( msg = getBranchMsg()))
        return content
    }

    override fun getBoundary(): Boundary = Boundary.PUBLIC

    override fun getReturn(): String? = "ResponseEntity"

    override fun getTags(): List<String> = listOf(
            "@${SpringAnnotation.REQUEST_MAPPING.getText(mapOf("value" to "", "method" to "RequestMethod.PATCH", "consumes" to "MediaType.APPLICATION_JSON"))}"
    )
}