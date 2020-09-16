package org.evomaster.resource.rest.generator.implementation.java.controller

import org.evomaster.resource.rest.generator.implementation.java.JavaMethod
import org.evomaster.resource.rest.generator.template.Boundary

/**
 * created by manzh on 2019-10-14
 */
class GetPreferredOutputFormat : JavaMethod() {
    override fun getParams(): Map<String, String>  = mapOf()
    override fun getBody(): List<String>  = listOf("return SutInfoDto.OutputFormat.JAVA_JUNIT_4;")

    override fun getName(): String  = "getPreferredOutputFormat"

    override fun getBoundary(): Boundary = Boundary.PUBLIC

    override fun getReturn(): String? = "SutInfoDto.OutputFormat"

    override fun getTags(): List<String> = listOf("@Override")
}