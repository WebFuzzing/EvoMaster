package org.evomaster.resource.rest.generator.implementation.java.controller.em

import org.evomaster.resource.rest.generator.implementation.java.JavaMethod
import org.evomaster.resource.rest.generator.template.Boundary


/**
 * created by manzh on 2019-10-14
 */
class StopSut : JavaMethod() {

    override fun getParams(): Map<String, String>  = mapOf()

    override fun getBody(): List<String> = listOf(
            "ctx.stop();",
            "ctx.close();"
    )

    override fun getName(): String = "stopSut"

    override fun getBoundary(): Boundary  = Boundary.PUBLIC

    override fun getTags(): List<String> = listOf("@Override")
}