package org.evomaster.resource.rest.generator.implementation.java.em

import org.evomaster.resource.rest.generator.implementation.java.JavaMethod
import org.evomaster.resource.rest.generator.template.Boundary

/**
 * created by manzh on 2019-10-14
 */
class GetProblemInfo : JavaMethod() {
    override fun getParams(): Map<String, String> = mapOf()

    override fun getBody(): List<String> =
            listOf(
                    """
                        return new RestProblem(
                            "http://localhost:" + getSutPort() + "/v2/api-docs", null);
                    """.trimIndent()
            )

    override fun getName(): String = "getProblemInfo"

    override fun getBoundary(): Boundary = Boundary.PUBLIC

    override fun getReturn(): String? = "ProblemInfo"

    override fun getTags(): List<String> = listOf("@Override")
}