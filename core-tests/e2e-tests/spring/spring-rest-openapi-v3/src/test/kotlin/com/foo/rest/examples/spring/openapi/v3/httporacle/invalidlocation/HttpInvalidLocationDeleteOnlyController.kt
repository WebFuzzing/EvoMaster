package com.foo.rest.examples.spring.openapi.v3.httporacle.invalidlocation.deleteonly

import com.foo.rest.examples.spring.openapi.v3.SpringController
import com.foo.rest.examples.spring.openapi.v3.httporacle.invalidlocation.deleteonly.HttpInvalidLocationDeleteOnlyApplication


class HttpInvalidLocationDeleteOnlyController: SpringController(HttpInvalidLocationDeleteOnlyApplication::class.java){

    override fun resetStateOfSUT() {
        HttpInvalidLocationDeleteOnlyApplication.reset()
    }
}
