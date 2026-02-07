package com.foo.spring.rest.mysql.sqli

import com.foo.spring.rest.mysql.SpringRestMySqlController
import com.foo.spring.rest.mysql.SpringRestMySqlSqliController
import com.foo.spring.rest.mysql.sqli.body.BodySQLiApplication
import org.evomaster.client.java.sql.DbSpecification

class SQLiMySQLBodyController : SpringRestMySqlSqliController(BodySQLiApplication::class.java)
