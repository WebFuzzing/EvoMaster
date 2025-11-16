package com.foo.rest.examples.spring.openapi.v3.security.sqli
import com.foo.rest.examples.spring.openapi.v3.SpringController
import com.foo.rest.examples.spring.openapi.v3.security.sqli.query.QuerySQLiApplication

class SQLiH2QueryController : SpringController(QuerySQLiApplication::class.java)