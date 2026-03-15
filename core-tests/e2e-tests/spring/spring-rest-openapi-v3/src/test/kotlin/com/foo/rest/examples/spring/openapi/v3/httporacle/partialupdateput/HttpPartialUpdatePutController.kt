package com.foo.rest.examples.spring.openapi.v3.httporacle.partialupdateput

import com.foo.rest.examples.spring.openapi.v3.SpringController


class HttpPartialUpdatePutController: SpringController(PartialUpdatePutApplication::class.java){

    override fun resetStateOfSUT() {
        PartialUpdatePutApplication.reset()
    }
}
