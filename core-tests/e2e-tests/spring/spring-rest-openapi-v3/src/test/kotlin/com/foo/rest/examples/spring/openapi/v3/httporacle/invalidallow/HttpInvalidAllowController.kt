package com.foo.rest.examples.spring.openapi.v3.httporacle.invalidallow

import com.foo.rest.examples.spring.openapi.v3.SpringController
import com.foo.rest.examples.spring.openapi.v3.httporacle.invalidallow.base.HttpInvalidAllowApplication


class HttpInvalidAllowController : SpringController(HttpInvalidAllowApplication::class.java) {

    override fun resetStateOfSUT() {
        HttpInvalidAllowApplication.reset()
    }
}
