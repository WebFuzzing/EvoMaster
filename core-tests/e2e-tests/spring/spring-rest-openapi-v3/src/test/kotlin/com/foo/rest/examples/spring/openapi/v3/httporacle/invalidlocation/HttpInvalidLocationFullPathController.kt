package com.foo.rest.examples.spring.openapi.v3.httporacle.invalidlocation.fullpath

import com.foo.rest.examples.spring.openapi.v3.SpringController
import com.foo.rest.examples.spring.openapi.v3.httporacle.invalidlocation.base.HttpInvalidLocationApplication
import com.foo.rest.examples.spring.openapi.v3.httporacle.invalidlocation.notvalidpath.HttpInvalidLocationNotValidApplication


class HttpInvalidLocationFullPathController: SpringController(HttpInvalidLocationFullPathApplication::class.java){

    override fun resetStateOfSUT() {
        HttpInvalidLocationFullPathApplication.reset()
    }
}
