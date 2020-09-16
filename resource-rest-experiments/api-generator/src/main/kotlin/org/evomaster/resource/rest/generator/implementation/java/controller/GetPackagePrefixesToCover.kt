package org.evomaster.resource.rest.generator.implementation.java.controller

import org.evomaster.resource.rest.generator.implementation.java.JavaMethod
import org.evomaster.resource.rest.generator.model.CommonTypes
import org.evomaster.resource.rest.generator.template.Boundary

/**
 * created by manzh on 2019-10-14
 */
class GetPackagePrefixesToCover(val packagePrefix : String) : JavaMethod(){
    override fun getParams(): Map<String, String> = mapOf()

    override fun getBody(): List<String>  = listOf(
            "return \"$packagePrefix\";"
    )

    override fun getBoundary(): Boundary  = Boundary.PUBLIC

    override fun getReturn(): String? = CommonTypes.STRING.toString()

    override fun getName(): String = "getPackagePrefixesToCover"

    override fun getTags(): List<String> = listOf("@Override")
}