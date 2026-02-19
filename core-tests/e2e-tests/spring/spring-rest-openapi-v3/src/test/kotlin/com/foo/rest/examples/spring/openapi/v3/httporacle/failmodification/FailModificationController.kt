package com.foo.rest.examples.spring.openapi.v3.httporacle.failmodification

import com.foo.rest.examples.spring.openapi.v3.SpringController


class FailModificationController: SpringController(FailModificationApplication::class.java){

    override fun resetStateOfSUT() {
        FailModificationApplication.reset()
    }
}
