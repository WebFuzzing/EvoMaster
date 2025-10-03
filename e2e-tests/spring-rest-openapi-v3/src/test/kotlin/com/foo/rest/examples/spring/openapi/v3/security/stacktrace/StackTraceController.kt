package com.foo.rest.examples.spring.openapi.v3.security.stacktrace

import com.foo.rest.examples.spring.openapi.v3.SpringController

class StackTraceController : SpringController(StackTraceApplication::class.java) {

    override fun resetStateOfSUT() {
        StackTraceApplication.reset()
    }

}