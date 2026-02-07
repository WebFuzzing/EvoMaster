package com.foo.spring.rest.mysql

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.client.java.sql.DbCleaner
import org.evomaster.client.java.sql.DbSpecification
import org.hibernate.dialect.MySQL8Dialect
import org.springframework.boot.SpringApplication
import org.springframework.jdbc.core.JdbcTemplate
import java.sql.DriverManager
import java.util.Arrays

open class SpringRestMySqlSqliController(applicationClass: Class<*>): SpringRestMySqlController(applicationClass) {
    override fun pathToFlywayFiles() = "classpath:/schema/sqli/data.sql"

    private val MYSQL_DB_NAME = "test"
    private val MYSQL_PORT = 3306

    private var dbSpecification: MutableList<DbSpecification?>? = null

    override fun startSut(): String {
        mysql.start()

        val host = mysql.getContainerIpAddress()
        val port = mysql.getMappedPort(MYSQL_PORT)
        val url = "jdbc:mysql://$host:$port/$MYSQL_DB_NAME"

        dbConnection = DriverManager.getConnection(url, "test", "test")


        ctx = SpringApplication.run(applicationClass,
            "--server.port=0",
            "--spring.datasource.url=$url",
            "--spring.jpa.database-platform="+ MySQL8Dialect::class.java.name,
            "--spring.datasource.username=test",
            "--spring.datasource.password=test",
            "--spring.flyway.enabled=false",
            "--spring.jpa.properties.hibernate.show_sql=true",
            "--spring.jpa.hibernate.ddl-auto=create-drop",
            "--spring.jmx.enabled=false"
        )!!


        dbConnection?.close()

        val jdbc = ctx!!.getBean(JdbcTemplate::class.java)
        dbConnection = jdbc.dataSource!!.connection

        dbSpecification = Arrays.asList<DbSpecification?>(
            DbSpecification(DatabaseType.MYSQL, dbConnection)
                .withInitSqlOnResourcePath(pathToFlywayFiles())
        )

        return "http://localhost:" + getSutPort()
    }

    override fun resetStateOfSUT() {
    }

    override fun getDbSpecifications(): MutableList<DbSpecification>? = dbSpecification as MutableList<DbSpecification>?
}