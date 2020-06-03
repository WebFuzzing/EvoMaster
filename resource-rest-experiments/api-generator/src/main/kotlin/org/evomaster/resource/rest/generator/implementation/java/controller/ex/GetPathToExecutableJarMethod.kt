package org.evomaster.resource.rest.generator.implementation.java.controller.ex

import org.evomaster.resource.rest.generator.implementation.java.JavaMethod
import org.evomaster.resource.rest.generator.template.Boundary

/**
 * created by manzh on 2020-01-02
 */
class GetPathToExecutableJarMethod(val jarLoc : String) : JavaMethod(){
    override fun getParams(): Map<String, String> = mapOf()

    override fun getBody(): List<String>  = listOf(
           """
               return $jarLoc;
           """.trimIndent()
    )

    override fun getBoundary(): Boundary = Boundary.PUBLIC

    override fun getReturn(): String? = "String"

    override fun getName(): String = "getPathToExecutableJar"

    override fun getTags(): List<String> = listOf("@Override")
}