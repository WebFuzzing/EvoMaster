package com.foo.rest.examples.spring.openapi.v3.httporacle.invalidmergepatch

import com.foo.rest.examples.spring.openapi.v3.SpringController

class InvalidMergePatchController : SpringController(InvalidMergePatchApplication::class.java) {

    override fun resetStateOfSUT() {
        InvalidMergePatchApplication.reset()
    }
}
