package org.evomaster.resource.rest.generator.implementation.java.controller.em

import org.evomaster.resource.rest.generator.implementation.java.JavaMethod
import org.evomaster.resource.rest.generator.template.Boundary

/**
 * created by manzh on 2019-10-14
 */
class GetSutPort :JavaMethod() {
    override fun getParams(): Map<String, String> = mapOf()

    override fun getBody(): List<String> {
        return listOf(
                """
                     return (Integer) ((Map) ctx.getEnvironment()
                        .getPropertySources().get("server.ports").getSource())
                        .get("local.server.port");
                    """.trimIndent()
        )
    }

    override fun getName(): String = "getSutPort"

    override fun getBoundary(): Boundary = Boundary.PROTECTED

    override fun getReturn(): String? = "int"
}