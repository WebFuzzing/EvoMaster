package org.evomaster.resource.rest.generator.implementation.java.controller.ex

import org.evomaster.resource.rest.generator.implementation.java.JavaMethod
import org.evomaster.resource.rest.generator.model.CommonTypes
import org.evomaster.resource.rest.generator.template.Boundary

/**
 * created by manzh on 2020-01-02
 */
class GetBaseURLMethod(val sutPort : String) : JavaMethod(){
    override fun getParams(): Map<String, String> = mapOf()

    override fun getBody(): List<String>  = listOf(
           """
               return "http://localhost:" + $sutPort;
           """.trimIndent()
    )

    override fun getBoundary(): Boundary = Boundary.PUBLIC

    override fun getReturn(): String? = CommonTypes.STRING.toString()

    override fun getName(): String = "getBaseURL"

    override fun getTags(): List<String> = listOf("@Override")
}