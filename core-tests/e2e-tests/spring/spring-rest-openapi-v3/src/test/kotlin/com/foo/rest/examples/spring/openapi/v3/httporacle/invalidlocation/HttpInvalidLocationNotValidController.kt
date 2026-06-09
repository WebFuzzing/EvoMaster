package com.foo.rest.examples.spring.openapi.v3.httporacle.invalidlocation.notvalidpath

import com.foo.rest.examples.spring.openapi.v3.SpringController
import com.foo.rest.examples.spring.openapi.v3.httporacle.invalidlocation.base.HttpInvalidLocationApplication
import com.foo.rest.examples.spring.openapi.v3.httporacle.invalidlocation.notvalidpath.HttpInvalidLocationNotValidApplication


class HttpInvalidLocationNotValidController: SpringController(HttpInvalidLocationNotValidApplication::class.java){

    override fun resetStateOfSUT() {
        HttpInvalidLocationNotValidApplication.reset()
    }
}
