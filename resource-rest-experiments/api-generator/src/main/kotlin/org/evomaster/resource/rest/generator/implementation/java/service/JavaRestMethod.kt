package org.evomaster.resource.rest.generator.implementation.java.service

import org.evomaster.resource.rest.generator.FormatUtil
import org.evomaster.resource.rest.generator.implementation.java.JavaMethod
import org.evomaster.resource.rest.generator.implementation.java.JavaUtils
import org.evomaster.resource.rest.generator.implementation.java.SpringAnnotation
import org.evomaster.resource.rest.generator.implementation.java.SpringRestAPI
import org.evomaster.resource.rest.generator.implementation.java.dependency.ConditionalDependency
import org.evomaster.resource.rest.generator.implementation.java.dependency.ConditionalDependencyKind
import org.evomaster.resource.rest.generator.implementation.java.entity.JavaE2DMethod
import org.evomaster.resource.rest.generator.model.*
import org.evomaster.resource.rest.generator.template.Boundary
import org.evomaster.resource.rest.generator.template.Tag

/**
 * created by manzh on 2019-12-19
 */
class JavaRestMethod (val specification: ServiceClazz, val method : RestMethod): JavaMethod(), SpringRestAPI{

    //by object
    private val dtoVar = FormatUtil.lowerFirst(specification.dto.name)
    private val entityId = "$dtoVar.${specification.dto.idProperty.name}"

    //by values
    private val values = specification.entity.defaultProperties.map{it.name}
    protected val idVar = "${FormatUtil.lowerFirst(specification.resourceName)}Id"
    private val valueVars = values.map { "${FormatUtil.lowerFirst(specification.resourceName)}$it" }

    private val referVars = specification.dto.referToOthers.map { Pair(it.name, it.type) }
    private val ownedVars = specification.dto.ownOthers.map { Pair(it.name, it.type)}
    private val ownedPVars = specification.dto.ownOthersProperties.map { s-> s.map { Pair(it.name, it.type) } }

    private val ifsnippet : MutableList<IfSnippet> =  mutableListOf()

    override fun getParamTag(): Map<String, String> {
        return when(method){
            RestMethod.POST ->{
                extraPathParamsTags(mapOf(dtoVar to SpringAnnotation.REQUEST_BODY.getText()).toMutableMap(), skip = listOf(idVar))
            }
            RestMethod.PUT, RestMethod.POST_ID ->{
                extraPathParamsTags(mapOf(
                        idVar to SpringAnnotation.PATH_VAR.getText(mapOf("name" to idVar)),
                        dtoVar to SpringAnnotation.REQUEST_BODY.getText()
                ).toMutableMap())
            }
            RestMethod.POST_VALUE ->{
                extraPathParamsTags(
                        mapOf(
                                idVar to SpringAnnotation.PATH_VAR.getText(mapOf("name" to idVar))
                        ).plus(valueVars.plus(referVars.plus(ownedVars).toMap().keys).plus(ownedPVars.flatMap { it.map { s->s.first } })
                                .map {valueVar-> valueVar to getTagText(valueVar) }
                        ).toMutableMap()
                )

            }
            RestMethod.DELETE ->{
                return mapOf(idVar to SpringAnnotation.PATH_VAR.getText(mapOf("name" to idVar)))
            }
            RestMethod.GET_ID, RestMethod.DELETE_CON -> extraPathParamsTags(mutableMapOf(idVar to SpringAnnotation.PATH_VAR.getText(mapOf("name" to idVar))))
            RestMethod.GET_ALL -> mapOf()
            RestMethod.GET_ALL_CON -> extraPathParamsTags(mutableMapOf(), listOf(idVar)) //skip current idVar
            RestMethod.PATCH_VALUE ->{
                /*
                    regarding patch method, it can update values of default properties, referred resources by id
                    and default properties of owned resources.  Since it does not allow to own an resource which
                    also owns/refer another resource, we don't need to handle the update of dependency of owned resources.
                */
                extraPathParamsTags(
                        mapOf(
                                idVar to SpringAnnotation.PATH_VAR.getText(mapOf("name" to idVar)))
                                .plus(valueVars.plus(referVars.toMap().keys).plus(ownedPVars.flatMap { it.map { s->s.first } })
                                        .map {valueVar-> valueVar to getTagText(valueVar, true) }
                                ).toMutableMap()
                )

            }
        }
    }

    private fun getTagText(name: String, patch : Boolean = false) : String{
        if(specification.pathParams.contains(name))
            return  SpringAnnotation.PATH_VAR.getText(mapOf("name" to name))
        return if (patch) SpringAnnotation.REQUEST_PARAM.getText(mapOf("name" to name, "required" to "false")) else SpringAnnotation.REQUEST_PARAM.getText(mapOf("name" to name))
    }

    private fun extraPathParams(current : MutableMap<String, String>, skip : List<String> = listOf()): Map<String, String> {
        val extra = specification.pathParams.filter { !current.keys.contains(it) && !skip.contains(it) }.sorted().map { it to specification.dto.idProperty.type }
        current.putAll(extra)
        return current
    }

    private fun extraPathParamsTags(current : MutableMap<String, String>, skip : List<String> = listOf()): Map<String, String> {
        val extra = specification.pathParams.filter { !current.keys.contains(it) && !skip.contains(it)}.sorted().map { it to SpringAnnotation.PATH_VAR.getText(mapOf("name" to it)) }
        current.putAll(extra)
        return current
    }

    override fun getParams(): Map<String, String> {
        return when(method){
            RestMethod.POST ->{
                extraPathParams(mapOf(dtoVar to "${specification.dto.name}").toMutableMap(), skip = listOf(idVar))
            }
            RestMethod.PUT, RestMethod.POST_ID-> {
                extraPathParams(mapOf(
                        idVar to specification.dto.idProperty.type,
                        dtoVar to "${specification.dto.name}"
                ).toMutableMap())
            }
            RestMethod.POST_VALUE->{
                extraPathParams(mapOf(idVar to specification.dto.idProperty.type)
                        .plus(valueVars.mapIndexed { index, d-> d to specification.entity.defaultProperties[index].type })
                        .plus(referVars.toMap())
                        .plus(ownedVars.toMap())
                        .plus(ownedPVars.flatten().toMap()).toMutableMap())
            }
            RestMethod.PATCH_VALUE->{
                extraPathParams(mapOf(idVar to specification.dto.idProperty.type)
                        .plus(valueVars.mapIndexed { index, d-> d to specification.entity.defaultProperties[index].type })
                        .plus(referVars.toMap())
                        .plus(ownedPVars.flatten().toMap()).toMutableMap())
            }
            RestMethod.DELETE ->{
                return mapOf(idVar to "${specification.dto.idProperty.type}")
            }
            RestMethod.GET_ALL -> mapOf()
            RestMethod.GET_ID, RestMethod.DELETE_CON -> extraPathParams(mutableMapOf(idVar to "${specification.dto.idProperty.type}"))
            RestMethod.GET_ALL_CON -> extraPathParams(mutableMapOf(), listOf(idVar))

        }
    }
    override fun getBoundary(): Boundary = Boundary.PUBLIC

    override fun getName(): String  = Utils.generateRestMethodName(method, specification.resourceName)

    override fun getBody(): List<String> {
        return getBodyByMethod()
    }

    private fun getBodyByMethod(): List<String> {
        // mark if branch with type
        val content = mutableListOf<String>()
        ifsnippet.clear()

        when(method){
            RestMethod.GET_ALL, RestMethod.GET_ALL_CON ->{
                val allDtos = "allDtos"
                content.add(findAllEntitiesAndConvertToDto(
                        specification.entityRepository.name, specification.entity.name, allDtos, JavaE2DMethod(specification.entity).getInvocation(null),specification.dto.name))
                content.add(returnWithContent(allDtos))
                return content
            }
            RestMethod.GET_ID ->{
                var dto = "dto"
                content.add(
                        findEntityByIdAndConvertToDto(
                                specification.entityRepository.name,
                                idVar, specification.entity.name, dto, specification.dto.name,
                                JavaE2DMethod(specification.entity).getInvocation(null),
                                ifsnippets = ifsnippet,
                                iftype = IfSnippetType.CHECK_OWN_EXISTENCE,
                                exceptionStatusCode = 404
                                )
                )
                content.add(returnWithContent(dto))
                return content
            }
            RestMethod.DELETE, RestMethod.DELETE_CON ->{
                content.add(assertExistence(specification.entityRepository.name, idVar, ifsnippet, IfSnippetType.CHECK_SELF_EXISTENCE, exceptionStatusCode = 404))
                content.add(repositoryDeleteById(specification.entityRepository.name, idVar))
                content.add(returnStatus(200)) // delete the resource succesfully
                return content
            }
        }

        val created = "node"

        val inputIsObject = (method == RestMethod.POST || method == RestMethod.PUT || method == RestMethod.POST_ID)


        //check id
        val idValue = if(!inputIsObject) idVar else entityId

        when(method){
            RestMethod.POST, RestMethod.POST_VALUE, RestMethod.POST_ID ->{
                if (method == RestMethod.POST_ID)
                    content.add(setIdOnPathToDto(idValue, idVar))
                content.add(assertNonExistence(specification.entityRepository.name, idValue, ifsnippet, IfSnippetType.CHECK_SELF_EXISTENCE, exceptionStatusCode = 400))
                content.add(formatInstanceClassAndAssigned(specification.entity.name, created, listOf()))
                content.add("$created.${specification.entity.idProperty.nameSetterName()}($idValue);")
            }

            RestMethod.PUT ->{
                content.add(setIdOnPathToDto(idValue, idVar))
                content.add(findOrCreateEntityByIdAndAssigned(
                        specification.entityRepository.name,
                        idValue,
                        created,
                        specification.entity.name,
                        formatInstanceClass(specification.entity.name, listOf()),
                        idSetter = "$created.${specification.entity.idProperty.nameSetterName()}($idValue);"
                ))
            }
            RestMethod.PATCH_VALUE->{
                content.add(findEntityByIdAndAssigned(
                        specification.entityRepository.name,
                        idValue,
                        created,
                        specification.entity.name,
                        ifsnippet,
                        IfSnippetType.CHECK_SELF_EXISTENCE,
                        exceptionStatusCode = "404"
                ))
            }

        }

        val referredEntity = mutableListOf<String>()
        val createdDtos = mutableListOf<String>()
        when(method){
            RestMethod.POST_VALUE, RestMethod.POST, RestMethod.PUT, RestMethod.POST_ID->{
                //handling owned resources
                val ownedId = specification.dto.ownOthers
                if(ownedId.isNotEmpty()){
                    content.add(JavaUtils.getSingleComment("create owned entity"))
                    (0 until ownedId.size).forEach { index->
                        val createdDtoVar = "ownedDto$index" //dto
                        createdDtos.add(createdDtoVar)
                        content.add(formatInstanceClassAndAssigned( specification.dto.ownOthersTypes[index], createdDtoVar, listOf()))
                        val statusCode = "${createdDtoVar}Code"

                        if (inputIsObject){
                            specification.dto.ownOthersProperties[index].plus(ownedId[index]).forEach { op->
                                val opp = op as? ResNodeTypedPropertySpecification?:throw IllegalArgumentException("wrong property spec")
                                content.add("$createdDtoVar.${opp.itsIdProperty.name}=$dtoVar.${op.name};")//check
                            }
//                            val apiVar = specification.ownedResourceService[specification.dto.ownOthersTypes[index]] as? ResServiceTypedPropertySpecification
//                                    ?:throw IllegalArgumentException("cannot find service to create the owned entity")
////                            content.add("int $statusCode = ${apiVar.name}.${Utils.generateRestMethodName(RestMethod.POST, apiVar.resourceName)}($createdDtoVar).getStatusCode().value();")
//                            val post = specification.ownedCreation[specification.dto.ownOthersTypes[index]]?: throw IllegalArgumentException("fail to find a creation for the owned entity")
//                            val inputs = if(post==RestMethod.POST) listOf(createdDtoVar) else if(post==RestMethod.POST_ID) listOf(idValue, createdDtoVar) else throw IllegalArgumentException("invalid post")
//                            content.add(createOwnedEntityWithServiceApi(statusCode, apiVar, post, inputs))
                        }else{
                            val varsOnParams = ownedPVars[index]
                            val opid = ownedId[index] as? ResNodeTypedPropertySpecification?:throw IllegalArgumentException("wrong property spec")
                            content.add("$createdDtoVar.${opid.itsIdProperty.name}=${ownedVars[index].first};")

                            specification.dto.ownOthersProperties[index].forEachIndexed { pi, op->
                                val opp = op as? ResNodeTypedPropertySpecification?:throw IllegalArgumentException("wrong property spec")
                                content.add("$createdDtoVar.${opp.itsIdProperty.name}=${varsOnParams[pi].first};")
                            }
//                            val apiVar = specification.ownedResourceService[specification.dto.ownOthersTypes[index]] as? ResServiceTypedPropertySpecification
//                                    ?:throw IllegalArgumentException("cannot find service to create the owned entity")
//
////                            content.add("int $statusCode = ${apiVar.name}.${Utils.generateRestMethodName(RestMethod.POST, apiVar.resourceName)}($createdDtoVar).getStatusCode().value();")
//                            val post = specification.ownedCreation[specification.dto.ownOthersTypes[index]]?: throw IllegalArgumentException("fail to find a creation for the owned entity")
//                            val inputs = if(post==RestMethod.POST) listOf(createdDtoVar) else if(post==RestMethod.POST_ID) listOf(idValue, createdDtoVar) else throw IllegalArgumentException("invalid post")
//                            content.add(createOwnedEntityWithServiceApi(statusCode, apiVar, post, inputs))
                        }

                        val apiVar = specification.ownedResourceService[specification.dto.ownOthersTypes[index]] as? ResServiceTypedPropertySpecification
                                ?:throw IllegalArgumentException("cannot find service to create the owned entity")
//                            content.add("int $statusCode = ${apiVar.name}.${Utils.generateRestMethodName(RestMethod.POST, apiVar.resourceName)}($createdDtoVar).getStatusCode().value();")
                        val post = specification.ownedCreation[specification.dto.ownOthersTypes[index]]?: throw IllegalArgumentException("fail to find a creation for the owned entity")
                        val inputs = if(post==RestMethod.POST) listOf(createdDtoVar) else if(post==RestMethod.POST_ID) listOf(idValue, createdDtoVar) else throw IllegalArgumentException("invalid post")
                        content.add(createOwnedEntityWithServiceApi(statusCode, apiVar, post, inputs))

                        val entityProperty = specification.entity.ownOthers[index]
                        val found = "ownedEntity${entityProperty.type}"
                        val id = if(inputIsObject) "$dtoVar.${ownedId[index].name}" else ownedVars[index].first
                        content.add(findEntityByIdAndAssigned(
                                specification.ownedEntityRepositories.getValue(entityProperty.type).name,
                                id,
                                found,
                                entityProperty.type,
                                ifsnippet,
                                IfSnippetType.CHECK_OWN_EXISTENCE,
                                exceptionStatusCode = statusCode // owned resource should be created by API with POST method
                        ))
                        content.add("$created.${entityProperty.nameSetterName()}($found);")
                    }
                }

                //handling referred resources
                if(specification.dto.referToOthers.isNotEmpty())
                    content.add(JavaUtils.getSingleComment("refer to related entity"))
                assert( specification.dto.referToOthers.size == specification.entity.referToOthers.size )
                (0 until specification.dto.referToOthers.size).forEach { index->
                    val entityProperty = specification.entity.referToOthers[index]
                    val dtoProperty = specification.dto.referToOthers[index]

                    val found = "referVarTo${entityProperty.type}"
                    val refer =
                            if(inputIsObject)
                                "$dtoVar.${dtoProperty.name}"
                            else
                                referVars[index].first

                    referredEntity.add(found)

                    content.add(findEntityByIdAndAssigned(
                            specification.obviousReferEntityRepositories.getValue(entityProperty.type).name,
                            refer,
                            found,
                            entityProperty.type,
                            ifsnippet,
                            IfSnippetType.CHECK_REFER_EXISTENCE
                    ))

                    content.add("$created.${entityProperty.nameSetterName()}($found);")
                }

            }
            RestMethod.PATCH_VALUE->{
                //handling owned resources
                val ownedId = specification.dto.ownOthers
                if(ownedId.isNotEmpty()){
                    content.add(JavaUtils.getSingleComment("update owned entity"))
                    (0 until ownedId.size).forEach { index->
                        val ownedResourceId = ownedId[index] as? ResNodeTypedPropertySpecification?:throw IllegalArgumentException("wrong property spec")
                        val idScript = "$created.${specification.entity.ownOthers[index].nameGetterName()}().${ownedResourceId.itsIdProperty.nameGetterName()}()"
                        val entityType = specification.entity.ownOthers[index].type
                        val findEntityVar = "ownedEntity$index"
                        val repository = specification.ownedEntityRepositories.getValue(entityType).name
                        val proVars =
                                if (inputIsObject)
                                    specification.dto.ownOthersProperties[index]
                                            .map { op-> "$dtoVar.${op.name}" }
                                else specification.dto.ownOthersProperties[index].
                                        mapIndexed { pi, _-> (ownedPVars[index])[pi].first }
                        val proSetterInEntity = specification.entity.ownOthersProperties[index].map { it.nameSetterName() }
                        content.add(anyPropertiesNotNullUpdateEntity(
                                repository = repository,
                                idScript = idScript,
                                entityType = entityType,
                                targetEntity = findEntityVar,
                                properties = proVars,
                                entitySetterProperties = proSetterInEntity,
                                ifsnippets = ifsnippet,
                                iftype = IfSnippetType.CHECK_OWN_EXISTENCE
                        ))
                        val createdDtoVar = "ownedDto$index" //dto
                        content.add(entityConvertToDto(
                                entityInstance = findEntityVar,
                                targetType = specification.dto.ownOthersTypes[index],
                                target = createdDtoVar,
                                toDtoMethod = "${ResGenSpecification.TODTO_METHOD_NAME}()"
                        ))
                        createdDtos.add(createdDtoVar)
                    }
                }

                //handling referred resources
                if(specification.dto.referToOthers.isNotEmpty())
                    content.add(JavaUtils.getSingleComment("refer to related entity"))
                assert( specification.dto.referToOthers.size == specification.entity.referToOthers.size )
                (0 until specification.dto.referToOthers.size).forEach { index->
                    val entityProperty = specification.entity.referToOthers[index]

                    val found = "referVarTo${entityProperty.type}"
                    val refer = referVars[index].first
                    referredEntity.add(found)

                    content.add(findEntityByIdAndAssignedAndSave(
                            specification.obviousReferEntityRepositories.getValue(entityProperty.type).name,
                            refer,
                            found,
                            entityProperty.type,
                            settingScript = "$created.${entityProperty.nameSetterName()}($found);",
                            gettingScript = "$created.${entityProperty.nameGetterName()}()",
                            ifsnippets = ifsnippet,
                            iftype = IfSnippetType.CHECK_REFER_EXISTENCE
                    ))
                }

            }
        }

        //instance branchMsg
        val withImpact = specification.entity.defaultProperties.any { it.impactful && it.branches > 1 }
        if (withImpact) content.add(initBranchesMessage())

        //handle assigment of property
        values.forEachIndexed { index, value ->
            val entityValue = specification.entity.defaultProperties.find {
                it.name == value
            }?: throw IllegalArgumentException("cannot find this property $value in entity")
            if(entityValue.impactful){
                val variableName = if(inputIsObject)"$dtoVar.${entityValue.name}" else valueVars[index]

                if(method == RestMethod.PATCH_VALUE){
                    val ifText = "if($variableName != null)"
                    ifsnippet.add(IfSnippet(ifText, IfSnippetType.CHECK_OPTIONAL_PARAM_EXISTENCE))
                    content.add("$ifText{")
                }
                //TODO mark if branch for branches setting
                content.add("$created.${entityValue.nameSetterName()}($variableName);")
                if (entityValue.branches > 1)
                    content.add(
                            defaultBranches(
                                    type = entityValue.type,
                                    index = index,
                                    variableName = variableName,
                                    numOfBranches = entityValue.branches
                            )
                    )
                if (method == RestMethod.PATCH_VALUE) content.add("}")
            }
        }

        //handle additional dependency with referred resources
        val additionalDep = specification.dto.referToOthers.find { it.dependency != ConditionalDependencyKind.EXISTENCE }?.dependency
        if(additionalDep != null){
            //all referred should be same
            if (!specification.dto.referToOthers.all { it.dependency == additionalDep})
                throw IllegalStateException("all referred resources should follow a same kind of dependency")
            if(specification.dto.referToOthers.isNotEmpty())
                content.add(JavaUtils.getSingleComment("additional codes for handling dependency "))
            //TODO property name
            val p = specification.dto.defaultProperties.find { it.forAdditionalDependency }
                    ?:specification.dto.ownOthersProperties
                            .map { e -> e.find { it is ResNodeTypedPropertySpecification && it.itsIdProperty.forAdditionalDependency } }.firstOrNull()
            if (p != null){
                //entity requires getter, dto direct access
                val probAs = if (createdDtos.isEmpty()) listOf("$created.${p.nameGetterName()}()") else createdDtos.map { "$it.${p.name}" }
                val probBs = referredEntity.map { "$it.${p.nameGetterName()}()" }
                content.add(assertCondition(ConditionalDependency.generateDependency(p.type, probAs, probBs), not = true, ifsnippets = ifsnippet, iftype = IfSnippetType.CHECK_REFER_PROPERTY))
            }
        }

        //handle save, delete or get the entity
        content.add(JavaUtils.getSingleComment("save the entity"))
        content.add(repositorySave(specification.entityRepository.name, created))

        //NOT APPLY this: Set the Location header to contain a link to the newly-created resource (on POST or PUT).

        val statusCode = when(method){
            RestMethod.PATCH_VALUE -> 200
            RestMethod.POST_VALUE, RestMethod.POST, RestMethod.POST_ID, RestMethod.PUT -> 201
            else -> throw IllegalArgumentException("NOT IMPLEMENT $method")
        }
        if (!withImpact) content.add(returnStatus(statusCode)) else content.add(returnStatus(statusCode, msg = getBranchMsg()))
        return content
    }

    fun getPath(): String{
        return when(method){
            RestMethod.GET_ALL_CON,RestMethod.POST -> specification.pathWithId
            RestMethod.PUT,  RestMethod.POST_ID, RestMethod.GET_ID, RestMethod.DELETE_CON,RestMethod.POST_VALUE,RestMethod.PATCH_VALUE -> "${specification.pathWithId}/{$idVar}"
            RestMethod.GET_ALL-> "/${specification.resourceOnPath}"
            RestMethod.DELETE-> "/${specification.resourceOnPath}/{$idVar}"
        }
    }

    override fun getTags(): List<String> {
        val map = when(method){
            RestMethod.POST,  RestMethod.POST_ID, RestMethod.PUT -> mapOf("value" to getPath(), "method" to "RequestMethod.${method.text}", "consumes" to "MediaType.APPLICATION_JSON")
            else -> mapOf("value" to getPath(), "method" to "RequestMethod.${method.text}", "produces" to "MediaType.APPLICATION_JSON")
        }
        return listOf(
                "@${SpringAnnotation.REQUEST_MAPPING.getText(map)}"
        )
    }

    override fun getReturn(): String? {
        return when(method){
            RestMethod.POST, RestMethod.POST_ID, RestMethod.POST_VALUE, RestMethod.PUT, RestMethod.PATCH_VALUE, RestMethod.DELETE, RestMethod.DELETE_CON -> "ResponseEntity"
            RestMethod.GET_ALL, RestMethod.GET_ALL_CON -> "ResponseEntity<List<${specification.dto.name}>>"
            RestMethod.GET_ID -> "ResponseEntity<${specification.dto.name}>"
        }
    }

    override fun getIfSnippets() : List<IfSnippet>{
        return ifsnippet.toList()
    }
}