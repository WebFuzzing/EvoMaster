package com.foo.rest.examples.spring.openapi.v3.security.sqli
import com.foo.rest.examples.spring.openapi.v3.SpringController
import com.foo.rest.examples.spring.openapi.v3.security.sqli.path.PathSQLiApplication

class SQLiH2PathController : SpringController(PathSQLiApplication::class.java)