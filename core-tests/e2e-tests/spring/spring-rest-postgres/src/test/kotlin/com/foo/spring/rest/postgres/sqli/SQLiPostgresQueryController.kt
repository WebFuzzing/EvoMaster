package com.foo.spring.rest.postgres.sqli

import com.foo.spring.rest.postgres.SpringRestPostgresSQLiController
import com.foo.spring.rest.postgres.sqli.query.QuerySQLiApplication

class SQLiPostgresQueryController : SpringRestPostgresSQLiController(QuerySQLiApplication::class.java)
