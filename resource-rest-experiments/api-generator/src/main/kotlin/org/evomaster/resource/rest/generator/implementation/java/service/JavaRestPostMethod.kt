package org.evomaster.resource.rest.generator.implementation.java.service

import org.evomaster.resource.rest.generator.FormatUtil
import org.evomaster.resource.rest.generator.implementation.java.JavaUtils
import org.evomaster.resource.rest.generator.implementation.java.SpringAnnotation
import org.evomaster.resource.rest.generator.implementation.java.dependency.ConditionalDependency
import org.evomaster.resource.rest.generator.implementation.java.dependency.ConditionalDependencyKind
import org.evomaster.resource.rest.generator.model.*
import org.evomaster.resource.rest.generator.template.Boundary

/**
 * created by manzh on 2019-08-15
 */
class JavaRestPostMethod(specification: ServiceClazz, method : RestMethod) : JavaRestMethod(specification, method){

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
        //check if the id exists
        content.add(JavaUtils.getSingleComment("check whether an resource exists by id"))
        content.add(assertNonExistence(specification.entityRepository.name, entityId))

        //instance an entity
        content.add(JavaUtils.getSingleComment("create the resources"))
        val created = "node"
        content.add(formatInstanceClassAndAssigned(specification.entity.name, created, listOf()))

        //set property regarding dto
        content.add("$created.${specification.entity.idProperty.nameSetterName()}($entityId);")

        val withImpact = specification.entity.defaultProperties.any { it.impactful && it.branches > 1 }
        if (withImpact){
            content.add(JavaUtils.getSingleComment("impactful property"))
            content.add(initBranchesMessage())
        }

        //check if the specified reference exists, if reference exists, then set it to the created
        val referredEntity = mutableListOf<String>()
        if(specification.dto.referToOthers.isNotEmpty())
            content.add(JavaUtils.getSingleComment("refer to related entity"))
        assert( specification.dto.referToOthers.size == specification.entity.referToOthers.size )
        (0 until specification.dto.referToOthers.size).forEach { index->
            val entityProperty = specification.entity.referToOthers[index]
            val dtoProperty = specification.dto.referToOthers[index]

            val found = "referVarTo${entityProperty.type}"
            val refer = "$dtoVar.${dtoProperty.name}"

            referredEntity.add(found)

            content.add(findEntityByIdAndAssigned(
                    specification.obviousReferEntityRepositories.getValue(entityProperty.type).name,
                    refer,
                    found,
                    entityProperty.type
            ))

            content.add("$created.${entityProperty.nameSetterName()}($found);")
        }

        //only set value if the property is impactful, following code is affected by existence of referred resource
        (0 until specification.entity.defaultProperties.size).forEach { i->
            val property = specification.entity.defaultProperties[i]
            if (property.impactful){
                val variableName = "$dtoVar.${property.name}"
                content.add("$created.${property.nameSetterName()}($variableName);")
                if (property.branches > 1){
                    content.add(defaultBranches(type = property.type, index =  i, numOfBranches = property.branches, variableName = variableName))
                }
            }
        }

        //create owned nodes
        val createdDtos = mutableListOf<String>()
        val ownedId = specification.dto.ownOthers
        val ownedProperties = specification.dto.ownOthersProperties
        val ownedTypes = specification.dto.ownOthersTypes
        if(ownedId.isNotEmpty()){
            content.add(JavaUtils.getSingleComment("create owned entity"))
            (0 until ownedId.size).forEach { index->
                val createdDtoVar = "ownedDto$index" //dto
                createdDtos.add(createdDtoVar)
                content.add(formatInstanceClassAndAssigned( ownedTypes[index], createdDtoVar, listOf()))
                ownedProperties[index].plus(ownedId[index]).forEach { op->
                    val opp = op as? ResNodeTypedPropertySpecification?:throw IllegalArgumentException("wrong property spec")
                    content.add("$createdDtoVar.${opp.itsIdProperty.name}=$dtoVar.${op.name};")//check
                }
                val apiVar = specification.ownedResourceService[ownedTypes[index]] as? ResServiceTypedPropertySpecification?:throw IllegalArgumentException("cannot find service to create the owned entity")

                content.add("${apiVar.name}.${Utils.generateRestMethodName(RestMethod.POST, apiVar.resourceName)}($createdDtoVar);")
                val entityProperty = specification.entity.ownOthers[index]
                val found = "ownedEntity${entityProperty.type}"
                val id = "$dtoVar.${ownedId[index].name}"
                content.add(findEntityByIdAndAssigned(
                        specification.ownedEntityRepositories.getValue(entityProperty.type).name,
                        id,
                        found,
                        entityProperty.type
                ))
                content.add("$created.${entityProperty.nameSetterName()}($found);")
            }
        }

        val additionalDep = specification.dto.referToOthers.find { it.dependency != ConditionalDependencyKind.EXISTENCE }?.dependency
        if(additionalDep != null){
            //all referred should be same
            if (!specification.dto.referToOthers.all { it.dependency == additionalDep})
                throw IllegalStateException("all referred resources should follow a same kind of dependency")
            if(specification.dto.referToOthers.isNotEmpty())
                content.add(JavaUtils.getSingleComment("additional codes for handling dependency "))
            //TODO property name
            val p = specification.dto.defaultProperties.find { it.forDependency }
                    ?:specification.dto.ownOthersProperties
                            .map { e -> e.find { it is ResNodeTypedPropertySpecification && it.itsIdProperty.forDependency } }.first()
            if (p != null){
                //entity requires getter, dto direct access
                val probAs = if (createdDtos.isEmpty()) listOf("$created.${p.nameGetterName()}()") else createdDtos.map { "$it.${p.name}" }
                val probBs = referredEntity.map { "$it.${p.nameGetterName()}()" }
                content.add(assertCondition(ConditionalDependency.generateDependency(p.type, probAs, probBs), not = true))
            }
        }

        //TODO regarding hide reference
        content.add(JavaUtils.getSingleComment("save the entity"))
        content.add(repositorySave(specification.entityRepository.name, created))
        if (!withImpact) content.add(returnStatus()) else content.add(returnStatus(msg = getBranchMsg()))
        return content
    }


    override fun getBoundary(): Boundary = Boundary.PUBLIC

    override fun getReturn(): String = "ResponseEntity"

    override fun getTags(): List<String> = listOf(
            "@${SpringAnnotation.REQUEST_MAPPING.getText(mapOf("value" to "", "method" to "RequestMethod.POST", "consumes" to "MediaType.APPLICATION_JSON"))}"
    )
}