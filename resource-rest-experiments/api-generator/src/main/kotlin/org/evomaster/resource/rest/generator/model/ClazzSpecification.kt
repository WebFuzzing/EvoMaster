package org.evomaster.resource.rest.generator.model

/**
 * created by manzh on 2019-08-19
 */
abstract class ClazzSpecification(
        val name : String,
        val properties : List<PropertySpecification>,
        val rootPackage : String,
        val outputFolder : String,
        val methods : MutableList<MethodSpecification> = mutableListOf()
)

/**
 * @property methodToDto first is a name of method, and second is return value.
 */
class EntityClazz(
        name: String,
        val resourceName: String,
        val idProperty: PropertySpecification,
        val defaultProperties : List<PropertySpecification>,
        val referToOthers: List<PropertySpecification>,
        val ownOthers: List<PropertySpecification>,
        val isATable : Boolean,
        val getDto : MethodSpecification?,
        val dto : DtoClazz,
        rootPackage: String,
        outputFolder: String,
        val idFromSuperClazz : Boolean
)
    : ClazzSpecification(name, defaultProperties.plus(referToOthers).plus(ownOthers).plus(idProperty), rootPackage, outputFolder)

class DtoClazz(
        name: String,
        val idProperty: PropertySpecification,
        val defaultProperties : List<PropertySpecification>,
        val referToOthers: List<PropertySpecification>,
        val ownOthers: List<PropertySpecification>,
        rootPackage: String,
        outputFolder: String,
        val idFromSuperClazz : Boolean
) : ClazzSpecification(name, defaultProperties.plus(referToOthers).plus(ownOthers).plus(idProperty), rootPackage, outputFolder)

class RepositoryClazz(
        name: String,
        val entityType : String,
        val idType : String,
        properties: List<PropertySpecification>,
        rootPackage: String,
        outputFolder: String) : ClazzSpecification(name, properties, rootPackage, outputFolder)

class ServiceClazz(
        name: String,
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
        val restMethods : List<RestMethod>,
        rootPackage: String,
        outputFolder: String
) : ClazzSpecification(name, obviousReferEntityRepositories.values.plus(entityRepository).plus(hideReferEntityRepositories.values), rootPackage, outputFolder)

class AppClazz(
        name: String = "ResApp",
        rootPackage: String,
        outputFolder: String
) : ClazzSpecification(name, listOf(), rootPackage, outputFolder )