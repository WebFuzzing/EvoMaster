package org.evomaster.resource.rest.generator.implementation.java.controller

import org.evomaster.resource.rest.generator.implementation.java.JavaDeclaration
import org.evomaster.resource.rest.generator.model.PropertySpecification
import org.evomaster.resource.rest.generator.template.Boundary

/**
 * created by manzh on 2019-10-14
 */
class SimplePrivateDeclaration(specification: PropertySpecification, val specifiedBoundary: Boundary = Boundary.PRIVATE, val isFinalProperty : Boolean = false) : JavaDeclaration(specification) {
    override fun getTags(): List<String> = listOf()

    override fun getBoundary(): Boundary = specifiedBoundary

    override fun isFinal(): Boolean  = isFinalProperty
}