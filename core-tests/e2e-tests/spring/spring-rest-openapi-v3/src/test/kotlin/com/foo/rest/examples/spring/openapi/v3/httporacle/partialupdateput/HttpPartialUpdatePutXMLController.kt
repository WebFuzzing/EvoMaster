package com.foo.rest.examples.spring.openapi.v3.httporacle.partialupdateput

import com.foo.rest.examples.spring.openapi.v3.SpringController
import com.foo.rest.examples.spring.openapi.v3.httporacle.partialupdateput.xml.PartialUpdatePutXMLApplication


class HttpPartialUpdatePutXMLController: SpringController(PartialUpdatePutXMLApplication::class.java){

    override fun resetStateOfSUT() {
        PartialUpdatePutXMLApplication.reset()
    }
}
