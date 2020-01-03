package org.evomaster.resource.rest.generator.implementation.java.controller.ex

import org.evomaster.resource.rest.generator.implementation.java.JavaMethod
import org.evomaster.resource.rest.generator.template.Boundary

/**
 * created by manzh on 2020-01-02
 */
class CloseDbConnectionMethod(val connection : String): JavaMethod() {

    override fun getParams(): Map<String, String> = mapOf()

    override fun getBody(): List<String> {
        return listOf("""
           if ($connection != null) {
              try {
                $connection.close();
              } catch (SQLException e) {
                e.printStackTrace();
              }
              $connection = null;
            }
        """.trimIndent())
    }

    override fun getName(): String = "closeDataBaseConnection"

    override fun getBoundary(): Boundary = Boundary.PRIVATE

    override fun getTags(): List<String> = listOf()

    override fun getReturn(): String? = null
}