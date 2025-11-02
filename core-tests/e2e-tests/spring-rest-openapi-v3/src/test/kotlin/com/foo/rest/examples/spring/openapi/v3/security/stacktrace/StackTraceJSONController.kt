package com.foo.rest.examples.spring.openapi.v3.security.stacktrace

import com.foo.rest.examples.spring.openapi.v3.SpringController
import com.foo.rest.examples.spring.openapi.v3.security.stacktrace.json.StackTraceJSONApplication

class StackTraceJSONController : SpringController(StackTraceJSONApplication::class.java) {

    override fun resetStateOfSUT() {
        StackTraceJSONApplication.reset()
    }

}
