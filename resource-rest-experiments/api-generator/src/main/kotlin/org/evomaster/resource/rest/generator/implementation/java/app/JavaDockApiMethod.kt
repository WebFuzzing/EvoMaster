package org.evomaster.resource.rest.generator.implementation.java.app

import org.evomaster.resource.rest.generator.implementation.java.JavaMethod
import org.evomaster.resource.rest.generator.implementation.java.SpringAnnotation
import org.evomaster.resource.rest.generator.implementation.java.service.IfSnippet
import org.evomaster.resource.rest.generator.template.Boundary

/**
 * created by manzh on 2019-08-20
 */
class JavaDockApiMethod : JavaMethod() {
    override fun getParams(): Map<String, String> = mapOf()

    override fun getBody(): List<String> = listOf(
            """
                return new Docket(DocumentationType.SWAGGER_2)
                    .apiInfo(apiInfo())
                    .select()
                    .paths(regex("/api/.*"))
                    .build()
                    .ignoredParameterTypes(WebRequest.class, Authentication.class);
            """.trimIndent()
    )

    override fun getReturn(): String? = "Docket"

    override fun getName(): String = "docketApi"

    override fun getBoundary(): Boundary = Boundary.PUBLIC

    override fun getTags(): List<String> = listOf(SpringAnnotation.BEAN.getText()).map { "@$it" }

    override fun getIfSnippets(): List<IfSnippet> = listOf()
}