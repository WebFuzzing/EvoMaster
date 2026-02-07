package com.foo.spring.rest.postgres.sqli

import com.foo.spring.rest.postgres.SpringRestPostgresSQLiController
import com.foo.spring.rest.postgres.sqli.path.PathSQLiApplication

class SQLiPostgresPathController : SpringRestPostgresSQLiController(PathSQLiApplication::class.java)
