package org.evomaster.resource.rest.generator.implementation.java.controller.ex

import org.evomaster.resource.rest.generator.implementation.java.JavaMethod
import org.evomaster.resource.rest.generator.template.Boundary

/**
 * created by manzh on 2020-01-02
 */
class GetJVMParametersMethod : JavaMethod(){
    override fun getParams(): Map<String, String> = mapOf()

    override fun getBody(): List<String>  = listOf(
            """
                return new String[]{
                    "-Dserver.port=" + sutPort,
                    //FIXME: re-enable once fixed issue with Spring
                    "-Dspring.datasource.url=" + dbUrl(true) + ";DB_CLOSE_DELAY=-1",
                    "-Dspring.datasource.driver-class-name=" + P6SpyDriver.class.getName(),
                    "-Dspring.jpa.database-platform=" + H2Dialect.class.getName(),
                    "-Dspring.datasource.username=sa",
                    "-Dspring.datasource.password",
                    "-Dspring.jpa.properties.hibernate.show_sql=true"
                };
            """.trimIndent()
    )

    override fun getBoundary(): Boundary = Boundary.PUBLIC

    override fun getReturn(): String? = "String[]"

    override fun getName(): String = "getJVMParameters"

    override fun getTags(): List<String> = listOf()
}