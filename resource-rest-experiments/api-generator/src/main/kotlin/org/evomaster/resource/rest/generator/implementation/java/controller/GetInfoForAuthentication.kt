package org.evomaster.resource.rest.generator.implementation.java.controller

import org.evomaster.resource.rest.generator.implementation.java.JavaMethod
import org.evomaster.resource.rest.generator.template.Boundary

/**
 * created by manzh on 2019-10-14
 */
class GetInfoForAuthentication : JavaMethod() {
    override fun getParams(): Map<String, String> = mapOf()

    override fun getBody(): List<String> {
        return listOf("return null;")
    }

    override fun getName(): String = "getInfoForAuthentication"

    override fun getBoundary(): Boundary = Boundary.PUBLIC

    override fun getReturn(): String?  = "List<AuthenticationDto>"

    override fun getTags(): List<String> = listOf("@Override")
}