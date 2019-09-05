package org.evomaster.resource.rest.generator.implementation.java

import org.evomaster.resource.rest.generator.model.PropertySpecification
import org.evomaster.resource.rest.generator.template.Boundary

/**
 * created by manzh on 2019-08-14
 */
class JavaGetterMethod (val specification: PropertySpecification) : JavaMethod() {

    override fun getReturn(): String? = specification.type

    override fun getParams(): Map<String, String> = mapOf()

    override fun getBody(): List<String> = listOf(
            "return this.${specification.name} ${statementEnd()}"
    )

    override fun getName(): String = specification.nameGetterName()

    override fun getBoundary(): Boundary = Boundary.PUBLIC
}