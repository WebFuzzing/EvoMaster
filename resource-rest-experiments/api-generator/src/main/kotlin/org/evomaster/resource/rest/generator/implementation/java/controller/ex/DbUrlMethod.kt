package org.evomaster.resource.rest.generator.implementation.java.controller.ex

import org.evomaster.resource.rest.generator.implementation.java.JavaMethod
import org.evomaster.resource.rest.generator.template.Boundary

/**
 * created by manzh on 2020-01-02
 */
class DbUrlMethod(val csName : String) : JavaMethod() {

    override fun getParams(): Map<String, String> = mutableMapOf("withP6Spy" to "boolean")

    override fun getBody(): List<String> {
        return listOf("""
            String url = "jdbc";
            if (withP6Spy) {
              url += ":p6spy";
            }
            url += ":h2:tcp://localhost:" + dbPort + "/./temp/tmp_$csName/testdb_" + dbPort;
        
            return url;
        """.trimIndent())
    }

    override fun getName(): String = "dbUrl"

    override fun getBoundary(): Boundary = Boundary.PRIVATE

    override fun getTags(): List<String> = listOf()

    override fun getReturn(): String? = "String"
}