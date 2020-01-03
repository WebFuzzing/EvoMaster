package org.evomaster.resource.rest.generator.implementation.java.service

import org.evomaster.resource.rest.generator.implementation.java.JavaDeclaration
import org.evomaster.resource.rest.generator.implementation.java.SpringAnnotation
import org.evomaster.resource.rest.generator.model.PropertySpecification
import org.evomaster.resource.rest.generator.template.Boundary

/**
 * created by manzh on 2019-08-15
 */
class JavaDeclarationRepository(specification: PropertySpecification) : JavaDeclaration(specification) {
    override fun getBoundary(): Boundary  = Boundary.PRIVATE

    override fun getTags(): List<String> = listOf(SpringAnnotation.AUTO_WIRED.getText())
}