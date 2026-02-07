package com.foo.spring.rest.mysql.sqli

import com.foo.spring.rest.mysql.SpringRestMySqlController
import com.foo.spring.rest.mysql.SpringRestMySqlSqliController
import com.foo.spring.rest.mysql.sqli.path.PathSQLiApplication
import org.evomaster.client.java.sql.DbSpecification

class SQLiMySQLPathController : SpringRestMySqlSqliController(PathSQLiApplication::class.java)
