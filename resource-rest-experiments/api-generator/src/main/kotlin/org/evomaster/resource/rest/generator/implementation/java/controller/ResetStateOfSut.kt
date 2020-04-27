package org.evomaster.resource.rest.generator.implementation.java.controller

import org.evomaster.resource.rest.generator.implementation.java.JavaMethod
import org.evomaster.resource.rest.generator.template.Boundary

/**
 * created by manzh on 2019-10-11
 */
class ResetStateOfSut(val connection : String) : JavaMethod() {

    override fun getParams(): Map<String, String> = mutableMapOf()

    override fun getBody(): List<String> {
        return listOf("DbCleaner.clearDatabase_H2($connection);")
    }

    override fun getName(): String = "resetStateOfSUT"

    override fun getBoundary(): Boundary = Boundary.PUBLIC

    override fun getTags(): List<String> = listOf("@Override")

}