package org.evomaster.resource.rest.generator.implementation.java.controller.em

import org.evomaster.resource.rest.generator.implementation.java.JavaMethod
import org.evomaster.resource.rest.generator.model.CommonTypes
import org.evomaster.resource.rest.generator.template.Boundary

/**
 * created by manzh on 2019-10-14
 */

class IsSutRunning : JavaMethod() {
    override fun getParams(): Map<String, String> = mapOf()

    override fun getBody(): List<String>  = listOf("return ctx != null && ctx.isRunning();")

    override fun getName(): String  = "isSutRunning"

    override fun getBoundary(): Boundary  = Boundary.PUBLIC

    override fun getReturn(): String? = CommonTypes.BOOLEAN.toString()

    override fun getTags(): List<String> = listOf("@Override")

}