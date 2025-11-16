package com.foo.rest.examples.spring.openapi.v3.security.sqli
import com.foo.rest.examples.spring.openapi.v3.SpringController
import com.foo.rest.examples.spring.openapi.v3.security.sqli.body.BodySQLiApplication

class SQLiH2BodyController : SpringController(BodySQLiApplication::class.java)