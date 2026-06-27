package com.foo.rest.examples.spring.openapi.v3.httporacle.partialupdateput

import com.foo.rest.examples.spring.openapi.v3.SpringController
import com.foo.rest.examples.spring.openapi.v3.httporacle.partialupdateput.urlencoded.PartialUpdatePutUrlEncodedApplication


class HttpPartialUpdatePutURLEncodedController: SpringController(PartialUpdatePutUrlEncodedApplication::class.java){

    override fun resetStateOfSUT() {
        PartialUpdatePutUrlEncodedApplication.reset()
    }
}
