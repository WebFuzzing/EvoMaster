package com.foo.spring.rest.mysql.sqli

import com.foo.spring.rest.mysql.SpringRestMySqlController
import com.foo.spring.rest.mysql.SpringRestMySqlSqliController
import com.foo.spring.rest.mysql.sqli.query.QuerySQLiApplication
import org.evomaster.client.java.sql.DbSpecification

class SQLiMySQLQueryController : SpringRestMySqlSqliController(QuerySQLiApplication::class.java)
