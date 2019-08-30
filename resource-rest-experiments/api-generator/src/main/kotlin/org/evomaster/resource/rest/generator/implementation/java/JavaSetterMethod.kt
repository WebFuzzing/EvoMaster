package org.evomaster.resource.rest.generator.implementation.java

import org.evomaster.resource.rest.generator.model.PropertySpecification
import org.evomaster.resource.rest.generator.template.Boundary

/**
 * created by manzh on 2019-08-14
 */
class JavaSetterMethod (val specification: PropertySpecification) : JavaMethod() {

    override fun getParams(): Map<String, String> = mapOf(specification.name to specification.type)

    override fun getBody(): List<String> = listOf(
            "this.${specification.name} = ${specification.name}${statementEnd()}"
    )

    override fun getName(): String = specification.nameSetterName()

    override fun getBoundary(): Boundary = Boundary.PUBLIC
}