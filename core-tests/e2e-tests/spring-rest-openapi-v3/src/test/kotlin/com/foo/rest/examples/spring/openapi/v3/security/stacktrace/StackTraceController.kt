package com.foo.rest.examples.spring.openapi.v3.security.stacktrace

import com.foo.rest.examples.spring.openapi.v3.SpringController
import com.foo.rest.examples.spring.openapi.v3.security.stacktrace.regular.StackTraceApplication

class StackTraceController : SpringController(StackTraceApplication::class.java) {

    override fun resetStateOfSUT() {
        StackTraceApplication.reset()
    }

}
