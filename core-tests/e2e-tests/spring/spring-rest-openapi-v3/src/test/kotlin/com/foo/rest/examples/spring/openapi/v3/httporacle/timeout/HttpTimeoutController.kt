package com.foo.rest.examples.spring.openapi.v3.httporacle.timeout

import com.foo.rest.examples.spring.openapi.v3.SpringController
import com.foo.rest.examples.spring.openapi.v3.httporacle.timeout.HttpTimeoutApplication


class HttpTimeoutController : SpringController(HttpTimeoutApplication::class.java) {

    override fun resetStateOfSUT() {
        HttpTimeoutApplication.reset()
    }
}
