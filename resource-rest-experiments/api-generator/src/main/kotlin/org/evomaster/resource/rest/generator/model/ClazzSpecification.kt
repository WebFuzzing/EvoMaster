package org.evomaster.resource.rest.generator.model

/**
 * created by manzh on 2019-08-19
 */
abstract class ClazzSpecification(
        val name : String,
        val properties : List<PropertySpecification>,
        val rootPackage : String,
        val outputFolder : String,
        val resourceFolder : String,
        val methods : MutableList<MethodSpecification> = mutableListOf()
)

/**
 * @property methodToDto first is a name of method, and second is return value.
 * @property ownOthersProperties only refer to properties of owned resources, which do not count as property of the entity!
 */
class EntityClazz(
        name: String,
        val resourceName: String,
        val idProperty: PropertySpecification,
        val defaultProperties : List<PropertySpecification>,
        val referToOthers: List<PropertySpecification>,
        val ownOthers: List<PropertySpecification>,
        val ownOthersProperties : List<List<PropertySpecification>> = listOf(),
        val isATable : Boolean,
        val getDto : MethodSpecification?,
        val dto : DtoClazz,
        rootPackage: String,
        outputFolder: String,
        resourceFolder: String,
        val idFromSuperClazz : Boolean
)
    : ClazzSpecification(name, defaultProperties.plus(referToOthers).plus(ownOthers).plus(idProperty), rootPackage, outputFolder, resourceFolder)

class DtoClazz(
        name: String,
        val idProperty: PropertySpecification,
        val defaultProperties : List<PropertySpecification>,
        val referToOthers: List<PropertySpecification>,
        val ownOthers: List<PropertySpecification>,
        val ownOthersProperties : List<List<PropertySpecification>> = listOf(),
        val ownOthersTypes: List<String> = listOf(),
        rootPackage: String,
        outputFolder: String,
        resourceFolder: String,
        val idFromSuperClazz : Boolean
) : ClazzSpecification(name, defaultProperties.plus(referToOthers).plus(ownOthers).plus(ownOthersProperties.flatten()).plus(idProperty), rootPackage, outputFolder,resourceFolder)

class RepositoryClazz(
        name: String,
        val entityType : String,
        val idType : String,
        properties: List<PropertySpecification>,
        rootPackage: String,
        outputFolder: String,
        resourceFolder: String) : ClazzSpecification(name, properties, rootPackage, outputFolder,resourceFolder)

class ServiceClazz(
        name: String,
        val resourceName: String,
        val entityRepository: PropertySpecification,
        val dto : DtoClazz,
        val entity : EntityClazz,
        val resourceOnPath : String,
        /**
         * key is type of refer entity
         * value is corresponding property specification
         */
        val obviousReferEntityRepositories : Map<String, PropertySpecification>,

        /**
         * key is type of refer entity
         * value is corresponding property specification
         */
        val hideReferEntityRepositories : Map<String, PropertySpecification>,

        /**
         * key is type of owned entity
         * value is corresponding property specification
         */
        val ownedEntityRepositories : Map<String, PropertySpecification>,

        /**
         * key is type of owned dto
         * value is corresponding property specification
         */
        val ownedResourceService : Map<String, PropertySpecification>,

        /**
         * key is type of owned dto
         * value is corresponding post type (POST or POST_ID) for creation
         */
        val ownedCreation : Map<String, RestMethod>,

        val restMethods : List<RestMethod>,
        rootPackage: String,
        outputFolder: String,
        resourceFolder: String,
        val path : String,
        val pathWithId : String,
        val pathParams : List<String>
) : ClazzSpecification(
        name,
        obviousReferEntityRepositories.values
                .plus(entityRepository)
                .plus(hideReferEntityRepositories.values)
                .plus(ownedEntityRepositories.values)
                .plus(ownedResourceService.values),
        rootPackage, outputFolder,resourceFolder)

class AppClazz(
        name: String = "ResApp",
        rootPackage: String,
        outputFolder: String,
        resourceFolder: String
) : ClazzSpecification(name, listOf(), rootPackage, outputFolder,resourceFolder )

class ServiceUtilClazz(
        name: String = "Util",
        rootPackage: String,
        outputFolder: String,
        resourceFolder: String
) : ClazzSpecification(name, listOf(), "$rootPackage.service", outputFolder,resourceFolder)