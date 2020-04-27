package org.evomaster.resource.rest.generator.implementation.java.controller.em

import org.evomaster.resource.rest.generator.implementation.java.JavaMethod
import org.evomaster.resource.rest.generator.model.CommonTypes
import org.evomaster.resource.rest.generator.template.Boundary

/**
 * created by manzh on 2019-10-11
 */
class StartSut(val appClazz: String) : JavaMethod() {

    override fun getParams(): Map<String, String> = mutableMapOf()

    override fun getBody(): List<String> {
        val content = """
            ctx = SpringApplication.run($appClazz.class, new String[]{
                "--server.port=0",
                "--spring.datasource.url=jdbc:p6spy:h2:mem:testdb;DB_CLOSE_DELAY=-1;",
                "--spring.datasource.driver-class-name=" + P6SpyDriver.class.getName(),
                "--spring.jpa.database-platform=" + H2Dialect.class.getName(),
                "--spring.datasource.username=sa",
                "--spring.datasource.password",
                "--spring.jpa.properties.hibernate.show_sql=true"
        });


        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        JdbcTemplate jdbc = ctx.getBean(JdbcTemplate.class);

        try {
            connection = jdbc.getDataSource().getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return "http://localhost:" + getSutPort();
        """

        return listOf(content)
    }

    override fun getName(): String = "startSut"

    override fun getBoundary(): Boundary = Boundary.PUBLIC

    override fun getTags(): List<String> = listOf("@Override")

    override fun getReturn(): String? = CommonTypes.STRING.toString()

}