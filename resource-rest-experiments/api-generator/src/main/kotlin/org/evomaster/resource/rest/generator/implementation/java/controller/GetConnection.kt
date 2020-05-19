package org.evomaster.resource.rest.generator.implementation.java.controller

import org.evomaster.resource.rest.generator.implementation.java.JavaMethod
import org.evomaster.resource.rest.generator.template.Boundary

/**
 * created by manzh on 2019-10-11
 */
class GetConnection(val connection : String) : JavaMethod() {

    override fun getParams(): Map<String, String> = mutableMapOf()

    override fun getBody(): List<String> {
        return listOf("return $connection;")
    }

    override fun getName(): String = "getConnection"

    override fun getBoundary(): Boundary = Boundary.PUBLIC

    override fun getTags(): List<String> = listOf("@Override")

    override fun getReturn(): String? = "Connection"
}