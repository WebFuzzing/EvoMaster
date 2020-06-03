package org.evomaster.resource.rest.generator.implementation.java.app

import org.evomaster.resource.rest.generator.implementation.java.JavaMethod
import org.evomaster.resource.rest.generator.implementation.java.service.IfSnippet
import org.evomaster.resource.rest.generator.model.AppClazz
import org.evomaster.resource.rest.generator.template.Boundary

/**
 * created by manzh on 2019-08-20
 */
class MainMethod (val specification : AppClazz): JavaMethod() {
    override fun getParams(): Map<String, String> = mapOf("args" to "String[]")

    override fun getBody(): List<String>  = listOf(
            """
               SpringApplication.run(${specification.name}.class, args);
            """.trimIndent()
    )

    override fun getName(): String = "main"

    override fun getBoundary(): Boundary  = Boundary.PUBLIC

    override fun isStatic(): Boolean = true

    override fun getComments(): List<String> = listOf(
            "//http://localhost:8080/v2/api-docs"
    )
}