package org.evomaster.resource.rest.generator.model

import org.evomaster.resource.rest.generator.FormatUtil

/**
 * created by manzh on 2019-08-19
 */
class ResGenSpecification(
        val resNode : ResNode,
        val doesMapToATable: Boolean = true,
        val rootPackage : String,
        val outputFolder : String,
        val restMethods : List<RestMethod>,
        val idProperty : PropertySpecification = PropertySpecification("id", CommonTypes.OBJ_LONG.name, isId = true, autoGen = false, allowNull = false),
        val defaultProperties : List<PropertySpecification> = mutableListOf(
                PropertySpecification("name", CommonTypes.STRING.name, isId = false, autoGen = false, allowNull = false),
                PropertySpecification("value", CommonTypes.INT.name, isId = false, autoGen = false, allowNull = false)
        ),
        val plusProperties: Boolean = true
){
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
                            allowNull = false
                    )
                },
                ownOthers = ownOthers.map {res ->
                    PropertySpecification(
                            name = res.nameOwnedResNodePropertyOnDto(),
                            type = idProperty.type,
                            isId = false,
                            autoGen = false,
                            allowNull = false
                    )
                },
                rootPackage = nameDtoPackage(),
                outputFolder =  outputFolder,
                idFromSuperClazz = !plusProperties
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
                            multiplicity = RelationMultiplicity.ONE_TO_ONE
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
                            multiplicity = RelationMultiplicity.ONE_TO_ONE
                    )
                },
                isATable = doesMapToATable,
                getDto = if (doesMapToATable) MethodSpecification("getDto", nameDtoClass(), mapOf()) else null,
                dto = getDto(),
                rootPackage = nameEntityPackage(),
                outputFolder = outputFolder,
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
                outputFolder = outputFolder
        )
        return repository
    }

    fun getApiService() : ServiceClazz?{
        if (!doesMapToATable) return null
        if (apiService != null) return apiService!!
        apiService = ServiceClazz(
                name = nameRestAPIClass(),
                resourceOnPath = nameResNodeOnPath(),
                entityRepository = PropertySpecification(
                        name = nameRepositoryClassVar(),
                        type = nameRepositoryClass(),
                        isId = false,
                        autoGen = false,
                        allowNull = false),
                dto = getDto(),
                entity = getEntity(),
                obviousReferEntityRepositories = obviousReferToOthers.map {r->
                    Pair(r.nameEntityClass(), PropertySpecification(
                            name = r.nameRepositoryClassVar(),
                            type = r.nameRepositoryClass(),
                            isId = false,
                            autoGen = false,
                            allowNull = false
                    ))
                }.toMap(),
                hideReferEntityRepositories = hideReferToOthers.map {r->
                    Pair(r.nameEntityClass(), PropertySpecification(
                            name = r.nameRepositoryClassVar(),
                            type = r.nameRepositoryClass(),
                            isId = false,
                            autoGen = false,
                            allowNull = false
                    ))
                }.toMap(),
                restMethods = restMethods,
                rootPackage = nameApiServicePackage(),
                outputFolder = outputFolder
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
    private fun nameResNodeOnPath() = "${FormatUtil.lowerFirst(name)}"

    private fun nameReferResNodePropertyOnDto() = "${FormatUtil.lowerFirst(name)}${FormatUtil.upperFirst(idProperty.name)}"

    private fun nameReferResNodePropertyOnEntity() = FormatUtil.lowerFirst(name)

    private fun nameOwnedResNodePropertyOnDto() = "owned${FormatUtil.lowerFirst(name)}${FormatUtil.upperFirst(idProperty.name)}"

    private fun nameOwnedResNodePropertyOnEntity() = "owned${FormatUtil.upperFirst(name)}"

}