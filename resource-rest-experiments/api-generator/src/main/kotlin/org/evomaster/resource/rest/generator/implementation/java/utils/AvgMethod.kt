package org.evomaster.resource.rest.generator.implementation.java.utils

import org.evomaster.resource.rest.generator.implementation.java.JavaMethod
import org.evomaster.resource.rest.generator.template.Boundary

/**
 * created by manzh on 2019-12-20
 */
class AvgMethod : JavaMethod() {
    override fun getParams(): Map<String, String> = mapOf("values" to "double[]")

    override fun getBody(): List<String>  = listOf(
            """
                double sum = 0;
                for (double v: values) sum += v;
                return sum/values.length;
            """.trimIndent()
    )

    override fun getName(): String = "average"

    override fun getReturn(): String? = "double"

    override fun getBoundary(): Boundary  = Boundary.PUBLIC

    override fun isStatic(): Boolean = true
}