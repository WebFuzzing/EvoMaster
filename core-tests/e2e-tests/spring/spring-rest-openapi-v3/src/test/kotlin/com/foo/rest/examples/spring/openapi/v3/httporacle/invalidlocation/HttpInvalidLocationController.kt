package com.foo.rest.examples.spring.openapi.v3.httporacle.invalidlocation

import com.foo.rest.examples.spring.openapi.v3.SpringController
import com.foo.rest.examples.spring.openapi.v3.httporacle.invalidlocation.base.HttpInvalidLocationApplication


class HttpInvalidLocationController: SpringController(HttpInvalidLocationApplication::class.java){

    override fun resetStateOfSUT() {
        HttpInvalidLocationApplication.reset()
    }
}
