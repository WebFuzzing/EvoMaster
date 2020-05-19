package org.evomaster.resource.rest.generator.implementation.java.controller.em

import org.evomaster.resource.rest.generator.implementation.java.JavaDeclaration
import org.evomaster.resource.rest.generator.model.PropertySpecification
import org.evomaster.resource.rest.generator.template.Boundary

/**
 * created by manzh on 2019-10-14
 */
class CTXDeclaration (specification: PropertySpecification): JavaDeclaration(specification){
    override fun getTags(): List<String> = listOf()

    override fun getBoundary(): Boundary = Boundary.PROTECTED

}