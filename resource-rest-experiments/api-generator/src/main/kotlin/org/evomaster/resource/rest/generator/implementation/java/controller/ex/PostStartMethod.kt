package org.evomaster.resource.rest.generator.implementation.java.controller.ex

import org.evomaster.resource.rest.generator.implementation.java.JavaMethod
import org.evomaster.resource.rest.generator.template.Boundary

/**
 * created by manzh on 2020-01-02
 */
class PostStartMethod : JavaMethod(){
    override fun getParams(): Map<String, String> = mapOf()

    override fun getBody(): List<String>  = listOf(
           """
               closeDataBaseConnection();

                try {
                  Class.forName("org.h2.Driver");
                  connection = DriverManager.getConnection(dbUrl(false), "sa", "");
                } catch (Exception e) {
                  throw new RuntimeException(e);
                }
           """.trimIndent()
    )

    override fun getBoundary(): Boundary = Boundary.PUBLIC

    override fun getReturn(): String? = null

    override fun getName(): String = "postStart"

    override fun getTags(): List<String> = listOf("@Override")
}