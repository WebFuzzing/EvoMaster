package org.evomaster.resource.rest.generator.implementation.java.app

import org.evomaster.resource.rest.generator.implementation.java.JavaMethod
import org.evomaster.resource.rest.generator.implementation.java.service.IfSnippet
import org.evomaster.resource.rest.generator.template.Boundary

/**
 * created by manzh on 2019-08-20
 */
class ApiInfoMethod : JavaMethod() {
    override fun getParams(): Map<String, String> = mapOf()

    override fun getBody(): List<String>  = listOf(
            """
                return new ApiInfoBuilder()
                    .title("Auto API for rest resources")
                    .description("description")
                    .version("2.0.0")
                    .build();
            """.trimIndent()
    )

    override fun getName(): String = "apiInfo"

    override fun getReturn(): String? = "ApiInfo"

    override fun getBoundary(): Boundary  = Boundary.PUBLIC
}