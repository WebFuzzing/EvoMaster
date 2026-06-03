package com.foo.rest.examples.spring.openapi.v3.httporacle.invalidlocation.locationget

import com.foo.rest.examples.spring.openapi.v3.SpringController
import com.foo.rest.examples.spring.openapi.v3.httporacle.invalidlocation.base.HttpInvalidLocationApplication
import com.foo.rest.examples.spring.openapi.v3.httporacle.invalidlocation.locationget.HttpInvalidLocationGetApplication


class HttpInvalidLocationGetController: SpringController(HttpInvalidLocationGetApplication::class.java){

    override fun resetStateOfSUT() {
        HttpInvalidLocationGetApplication.reset()
    }
}
