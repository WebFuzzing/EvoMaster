package org.evomaster.resource.rest.generator.implementation.java.controller.ex

import org.evomaster.resource.rest.generator.implementation.java.JavaMethod
import org.evomaster.resource.rest.generator.template.Boundary

/**
 * created by manzh on 2020-01-02
 */
class PostStopMethod(val h2 : String) : JavaMethod(){
    override fun getParams(): Map<String, String> = mapOf()

    override fun getBody(): List<String>  = listOf(
           """
               if ($h2 != null) {
                  $h2.stop();
                }
           """.trimIndent()
    )

    override fun getBoundary(): Boundary = Boundary.PUBLIC

    override fun getReturn(): String? = null

    override fun getName(): String = "postStop"

    override fun getTags(): List<String> = listOf("@Override")
}