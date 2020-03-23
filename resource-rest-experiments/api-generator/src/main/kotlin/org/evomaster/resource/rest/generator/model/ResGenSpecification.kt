package org.evomaster.resource.rest.generator.model

import org.evomaster.resource.rest.generator.FormatUtil
import org.evomaster.resource.rest.generator.implementation.java.dependency.ConditionalDependencyKind

/**
 * created by manzh on 2019-08-19
 */
class ResGenSpecification(
        val resNode : ResNode,
        val doesMapToATable: Boolean = true,
        val rootPackage : String,
        val outputFolder : String,
        val resourceFolder : String,
        val restMethods : List<RestMethod>,
        val createMethod: RestMethod,
        val idProperty : PropertySpecification ,
        val defaultProperties : List<PropertySpecification> = listOf(),
        val plusProperties: Boolean = true,
        val dependencyKind: ConditionalDependencyKind,
        val path : String,
        val pathWithId : String,
        val pathParams : List<String>
){
    companion object{
        const val TODTO_METHOD_NAME = "getDto"
    }
    val name : String = resNode.name
    private val hideReferToOthers : MutableList<ResGenSpecification> = mutableListOf()
    private val obviousReferToOthers : MutableList<ResGenSpecification> = mutableListOf()
    private val ownOthers : MutableList<ResGenSpecification> = mutableListOf()

    private var dto : DtoClazz? = null
    private var entity : EntityClazz? = null
    private var repository : RepositoryClazz? = null
    private var apiService : ServiceClazz? = null

    fun initDependence(resourceCluster : Map<String, ResGenSpecification>){
        if (resNode is MultipleResNode){
            resNode.nodes.forEach { n->
                val node = resourceCluster[n.name]?:throw IllegalArgumentException("resource node ${n.name} does not exist!")
                ownOthers.add(node)
            }
        }
        resNode.outgoing.forEach { e->
            val node = resourceCluster[e.target.name]?:throw IllegalArgumentException("resource node ${e.target.name} does not exist!")
            if (e.isHide)
                hideReferToOthers.add(node)
            else
                obviousReferToOthers.add(node)
        }
    }

    fun produceTypes() : List<String> = listOf(
            nameDtoClass(),
            nameEntityClass(),
            nameRepositoryClass(),
            nameRestAPIClass()
    )

    fun getDto() : DtoClazz{
        if (dto != null) return dto!!
        dto = DtoClazz(
                name = nameDtoClass(),
                idProperty = idProperty,
                defaultProperties = if(plusProperties) defaultProperties else listOf(),
                referToOthers = obviousReferToOthers.map { res->
                    PropertySpecification(
                            name = res.nameReferResNodePropertyOnDto(),
                            type = idProperty.type,
                            isId = false,
                            autoGen = false,
                            allowNull = false,
                            impactful = true,
                            dependency = dependencyKind
                    )
                },
                ownOthers = ownOthers.map {res ->
                    ResNodeTypedPropertySpecification(
                            name = res.nameOwnedResNodePropertyOnDto(),
                            type = idProperty.type,
                            itsIdProperty = res.idProperty,
                            isId = false,
                            autoGen = false,
                            allowNull = false,
                            impactful = true
                    )
                },
                ownOthersProperties = ownOthers.map { res ->
                    res.namePropertiesResNodePropertyOnDto().map { p ->
                        ResNodeTypedPropertySpecification(
                                name = p.first,
                                type = p.second.first,
                                itsIdProperty = p.second.second,
                                isId = false,
                                autoGen = false,
                                allowNull = false,
                                multiplicity = RelationMultiplicity.ONE_TO_ONE
                        )
                    }
                },
                ownOthersTypes = ownOthers.map { it.nameDtoClass() },
                rootPackage = nameDtoPackage(),
                outputFolder =  outputFolder,
                idFromSuperClazz = !plusProperties,
                resourceFolder = resourceFolder
                )
        return dto!!
    }

    fun getEntity() : EntityClazz{
        if (entity != null) return entity!!

        entity = EntityClazz(
                name= nameEntityClass(),
                resourceName = name,
                idProperty = idProperty,
                defaultProperties = if(plusProperties) defaultProperties else listOf(),
                referToOthers = obviousReferToOthers.map { res->
                    ResNodeTypedPropertySpecification(
                            name = res.nameReferResNodePropertyOnEntity(),
                            type = res.nameEntityClass(),
                            itsIdProperty = res.idProperty,
                            isId = false,
                            autoGen = false,
                            allowNull = false,
                            multiplicity = RelationMultiplicity.ONE_TO_ONE,
                            dependency = dependencyKind
                    )
                },
                ownOthers = ownOthers.map {res ->
                    ResNodeTypedPropertySpecification(
                            name = res.nameOwnedResNodePropertyOnEntity(),
                            type = res.nameEntityClass(),
                            itsIdProperty = res.idProperty,
                            isId = false,
                            autoGen = false,
                            allowNull = false,
                            multiplicity = RelationMultiplicity.ONE_TO_ONE,
                            ownedBy = nameEntityClass()
                    )
                },
                //refer to properties of owned resources
                ownOthersProperties = ownOthers.map { res ->
                    res.defaultProperties
                },
                isATable = doesMapToATable,
                getDto = if (doesMapToATable) MethodSpecification(TODTO_METHOD_NAME, nameDtoClass(), mapOf()) else null,
                dto = getDto(),
                rootPackage = nameEntityPackage(),
                outputFolder = outputFolder,
                resourceFolder = resourceFolder,
                idFromSuperClazz = !plusProperties)
        return entity!!
    }

    fun getRepository() : RepositoryClazz? {
        if (!doesMapToATable) return null
        if (repository != null) return repository!!

        repository = RepositoryClazz(
                name = nameRepositoryClass(),
                entityType = nameEntityClass(),
                //TODO("entity id type")
                idType = idProperty.type,
                properties = listOf(),
                rootPackage = nameEntityPackage(),
                outputFolder = outputFolder,
                resourceFolder = resourceFolder
        )
        return repository
    }

    fun getApiService() : ServiceClazz?{
        if (!doesMapToATable) return null
        if (apiService != null) return apiService!!
        apiService = ServiceClazz(
                name = nameRestAPIClass(),
                resourceName = name,
                resourceOnPath = FormatUtil.formatResourceOnPath(name),
                entityRepository = PropertySpecification(
                        name = nameRepositoryClassVar(),
                        type = nameRepositoryClass(),
                        isId = false,
                        autoGen = false,
                        allowNull = false,
                        impactful = true),
                dto = getDto(),
                entity = getEntity(),
                obviousReferEntityRepositories = obviousReferToOthers.map {r->
                    Pair(r.nameEntityClass(), PropertySpecification(
                            name = r.nameRepositoryClassVar(),
                            type = r.nameRepositoryClass(),
                            isId = false,
                            autoGen = false,
                            allowNull = false,
                            impactful = true
                    ))
                }.toMap(),
                hideReferEntityRepositories = hideReferToOthers.map {r->
                    Pair(r.nameEntityClass(), PropertySpecification(
                            name = r.nameRepositoryClassVar(),
                            type = r.nameRepositoryClass(),
                            isId = false,
                            autoGen = false,
                            allowNull = false,
                            impactful = true
                    ))
                }.toMap(),
                ownedEntityRepositories = ownOthers.map {r->
                    Pair(r.nameEntityClass(), PropertySpecification(
                            name = r.nameRepositoryClassVar(),
                            type = r.nameRepositoryClass(),
                            isId = false,
                            autoGen = false,
                            allowNull = false,
                            impactful = true
                    ))
                }.toMap(),
                ownedResourceService = ownOthers.map {r->
                    Pair(r.nameDtoClass(), ResServiceTypedPropertySpecification(
                            name = r.nameRestAPIClassVar(),
                            type = r.nameRestAPIClass(),
                            resourceName = r.name,
                            isId = false,
                            autoGen = false,
                            allowNull = false,
                            impactful = true
                    ))
                }.toMap(),
                ownedCreation = ownOthers.map {r->
                    Pair(r.nameDtoClass(), r.createMethod)
                }.toMap(),
                restMethods = restMethods,
                rootPackage = nameApiServicePackage(),
                outputFolder = outputFolder,
                resourceFolder = resourceFolder,
                path = path,
                pathWithId = pathWithId,
                pathParams = pathParams
        )

        return apiService!!
    }

    private fun nameEntityPackage() = "$rootPackage.entity"

    private fun nameDtoPackage() = "$rootPackage.dto"

    private fun nameApiServicePackage() = "$rootPackage.service"

    //for entity
    private fun nameEntityClass() = "${FormatUtil.upperFirst(name)}Entity"

    //for dto
    fun nameDtoClass() = "${FormatUtil.upperFirst(name)}"

    //for repository
    private fun nameRepositoryClass() = "${FormatUtil.upperFirst(name)}Repository"
    private fun nameRepositoryClassVar() = "${FormatUtil.lowerFirst(name)}Repository"

    //for restAPI
    private fun nameRestAPIClass() = "${FormatUtil.upperFirst(name)}RestAPI"
    private fun nameRestAPIClassVar() = "${FormatUtil.lowerFirst(name)}RestAPI"

    private fun nameReferResNodePropertyOnDto() = "${FormatUtil.lowerFirst(name)}${FormatUtil.upperFirst(idProperty.name)}"

    private fun nameReferResNodePropertyOnEntity() = FormatUtil.lowerFirst(name)

    private fun nameOwnedResNodePropertyOnDto() = "${FormatUtil.lowerFirst(name)}${FormatUtil.upperFirst(idProperty.name)}"

    private fun namePropertiesResNodePropertyOnDto() = defaultProperties.map {p->
        "${FormatUtil.lowerFirst(name)}${FormatUtil.upperFirst(p.name)}" to Pair(p.type, p)
    }

    private fun nameOwnedResNodePropertyOnEntity() = "owned${FormatUtil.upperFirst(name)}"

}