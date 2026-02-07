package com.foo.spring.rest.postgres.sqli

import com.foo.spring.rest.postgres.SpringRestPostgresSQLiController
import com.foo.spring.rest.postgres.sqli.body.BodySQLiApplication

class SQLiPostgresBodyController : SpringRestPostgresSQLiController(BodySQLiApplication::class.java)
