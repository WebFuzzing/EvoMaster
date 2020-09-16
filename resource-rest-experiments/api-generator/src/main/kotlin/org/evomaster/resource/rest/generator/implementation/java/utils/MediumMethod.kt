package org.evomaster.resource.rest.generator.implementation.java.utils

import org.evomaster.resource.rest.generator.implementation.java.JavaMethod
import org.evomaster.resource.rest.generator.template.Boundary

/**
 * created by manzh on 2019-12-20
 */
class MediumMethod : JavaMethod() {
    override fun getParams(): Map<String, String> = mapOf("values" to "double[]")

    override fun getBody(): List<String>  = listOf(
            """
                Arrays.sort(values);
                double median = 0;
                double pos1 = Math.floor((values.length - 1.0) / 2.0);
                double pos2 = Math.ceil((values.length - 1.0) / 2.0);
                if (pos1 == pos2 ) {
                    median = values[(int)pos1];
                } else {
                    median = (values[(int)pos1] + values[(int)pos2]) / 2.0 ;
                }
                return median;
            """.trimIndent()
    )

    override fun getName(): String = "medium"

    override fun getReturn(): String? = "double"

    override fun getBoundary(): Boundary  = Boundary.PUBLIC

    override fun isStatic(): Boolean = true
}