package org.evomaster.resource.rest.generator.implementation.java.entity

import org.evomaster.resource.rest.generator.implementation.java.JavaDeclaration
import org.evomaster.resource.rest.generator.implementation.java.JavaxAnnotation
import org.evomaster.resource.rest.generator.model.PropertySpecification
import org.evomaster.resource.rest.generator.model.RelationMultiplicity
import org.evomaster.resource.rest.generator.template.Boundary

/**
 * created by manzh on 2019-08-15
 */
class JavaDeclarationEntity (specification: PropertySpecification): JavaDeclaration(specification) {
    override fun getTags(): List<String> {
        val tags = mutableListOf<String>()
        if (this.specification.isId) tags.add(JavaxAnnotation.ID.getText())
        if (!this.specification.allowNull) tags.add(JavaxAnnotation.NOT_Null.getText())
        if (this.specification.autoGen) tags.add(JavaxAnnotation.AUTO_GEN_VALUE.getText())
        when(this.specification.multiplicity){
            RelationMultiplicity.MANY_TO_MANY -> tags.add(JavaxAnnotation.MANY_TO_MANY.getText())
            RelationMultiplicity.MANY_TO_ONE -> tags.add(JavaxAnnotation.MANY_TO_ONE.getText())
            RelationMultiplicity.ONE_TO_ONE -> {
                if(specification.ownedBy == null) tags.add(JavaxAnnotation.ONE_TO_ONE.getText())
                else tags.add(JavaxAnnotation.ONE_TO_ONE.getTextForOwnedResource(specification.ownedBy))
            }
            RelationMultiplicity.ONE_TO_MANY -> {
                if(specification.ownedBy == null) tags.add(JavaxAnnotation.ONE_TO_MANY.getText())
                else tags.add(JavaxAnnotation.ONE_TO_MANY.getTextForOwnedResource(specification.ownedBy))
            }
            else->{}
        }
        return tags.toList()
    }

    override fun getBoundary(): Boundary = Boundary.PRIVATE
}