package org.evomaster.resource.rest.generator.model

import org.evomaster.resource.rest.generator.FormatUtil
import org.evomaster.resource.rest.generator.implementation.java.dependency.ConditionalDependencyKind

/**
 * created by manzh on 2019-08-14
 */
open class PropertySpecification(
        val name: String,
        val type: String,
        val isId: Boolean,
        val autoGen: Boolean = false,
        val allowNull: Boolean = true,
        val multiplicity: RelationMultiplicity = RelationMultiplicity.NONE,
        val defaultValue: String? = null,
        val impactful: Boolean = true,
        val branches: Int = 0,
        val dependency: ConditionalDependencyKind = ConditionalDependencyKind.EXISTENCE,
        val forAdditionalDependency : Boolean = false,
        val ownedBy : String? = null
){

    /**
     * used in constructor
     */
    fun namePropertyVar() = "${name}Var"

    fun copy(name : String? = null) : PropertySpecification {
        return PropertySpecification(name
                ?: this.name, this.type, this.isId, this.autoGen, this.allowNull, this.multiplicity, this.defaultValue, this.impactful, this.branches, this.dependency, this.forAdditionalDependency, this.ownedBy)
    }

    fun nameGetterName() = "get${FormatUtil.upperFirst(name)}"

    fun nameSetterName() = "set${FormatUtil.upperFirst(name)}"
}

class ResNodeTypedPropertySpecification(
        name: String,
        type: String,
        val itsIdProperty : PropertySpecification,
        isId : Boolean,
        autoGen : Boolean = false,
        allowNull : Boolean = true,
        multiplicity: RelationMultiplicity = RelationMultiplicity.NONE,
        defaultValue : String? = null,
        impactful: Boolean = true,
        branches: Int = 0,
        dependency: ConditionalDependencyKind = ConditionalDependencyKind.EXISTENCE,
        ownedBy: String? = null
) : PropertySpecification(name, type, isId, autoGen, allowNull, multiplicity, defaultValue, impactful, branches, dependency, forAdditionalDependency = false, ownedBy = ownedBy)

class ResServiceTypedPropertySpecification(
        name: String,
        type: String,
        val resourceName : String,
        isId : Boolean,
        autoGen : Boolean = false,
        allowNull : Boolean = true,
        multiplicity: RelationMultiplicity = RelationMultiplicity.NONE,
        defaultValue : String? = null,
        impactful: Boolean = true,
        branches: Int = 0,
        dependency: ConditionalDependencyKind = ConditionalDependencyKind.EXISTENCE
) : PropertySpecification(name, type, isId, autoGen, allowNull, multiplicity, defaultValue, impactful, branches,dependency, forAdditionalDependency = false)