package org.evomaster.resource.rest.generator.implementation.java.dto

import org.evomaster.resource.rest.generator.implementation.java.JavaDeclaration
import org.evomaster.resource.rest.generator.implementation.java.SwaggerAnnotation
import org.evomaster.resource.rest.generator.model.PropertySpecification
import org.evomaster.resource.rest.generator.template.Boundary

/**
 * created by manzh on 2019-08-15
 */
class JavaDeclarationDto (specification: PropertySpecification): JavaDeclaration(specification) {
    override fun getTags(): List<String> {
        val tags = mutableListOf<String>()
        //if (this.specification.isId || !this.specification.allowNull)
        tags.add(SwaggerAnnotation.API_MODEL_PROPERTY.getText(mapOf("required" to specification.impactful.toString())))

        return tags.toList()
    }

    override fun getBoundary(): Boundary = Boundary.PUBLIC
}