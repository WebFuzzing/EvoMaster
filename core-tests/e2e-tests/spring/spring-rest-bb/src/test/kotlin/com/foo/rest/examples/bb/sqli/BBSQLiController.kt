package com.foo.rest.examples.bb.sqli

import com.foo.rest.examples.bb.SpringController
import org.evomaster.client.java.controller.api.dto.SutInfoDto
import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.client.java.sql.DbSpecification
import org.springframework.boot.SpringApplication
import org.springframework.jdbc.core.JdbcTemplate
import org.testcontainers.containers.GenericContainer
import java.sql.Connection


class BBSQLiController : SpringController(BBQuerySQLiApplication::class.java)
