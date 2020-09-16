package org.evomaster.resource.rest.generator.implementation.java.controller.ex

import org.evomaster.resource.rest.generator.implementation.java.JavaMethod
import org.evomaster.resource.rest.generator.model.CommonTypes
import org.evomaster.resource.rest.generator.template.Boundary

/**
 * created by manzh on 2020-01-02
 */
class GetLogMsgOfInitServerMethod(val appClazz : String) : JavaMethod(){
    override fun getParams(): Map<String, String> = mapOf()

    override fun getBody(): List<String>  = listOf(
           """
               return "Started $appClazz in ";
           """.trimIndent()
    )

    override fun getBoundary(): Boundary = Boundary.PUBLIC

    override fun getReturn(): String? = CommonTypes.STRING.toString()

    override fun getName(): String = "getLogMessageOfInitializedServer"

    override fun getTags(): List<String> = listOf("@Override")
}